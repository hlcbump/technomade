package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.chatbot.*;
import br.com.technomade.ecommerce.model.Categoria;
import br.com.technomade.ecommerce.model.Cliente;
import br.com.technomade.ecommerce.model.Compra;
import br.com.technomade.ecommerce.model.Produto;
import br.com.technomade.ecommerce.repository.CompraRepository;
import br.com.technomade.ecommerce.repository.ProdutoRepository;
import br.com.technomade.ecommerce.repository.spec.ProdutoSpecifications;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AnthropicClient anthropicClient;

    @Value("${chatbot.anthropic.model}")
    private String modelo;

    @Value("${chatbot.anthropic.max-tokens}")
    private Long maxTokens;

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "para", "por", "com", "sem", "um", "uma", "uns", "umas",
            "o", "a", "os", "as", "e", "ou", "que", "se", "ao", "aos",
            "me", "te", "lhe", "vos", "eu", "tu", "ele", "ela",
            "isso", "isto", "esse", "essa", "este", "esta", "mais", "muito",
            "tambem", "ja", "ainda", "so", "como", "mas", "porem", "porque",
            "quando", "onde", "quem", "qual", "quais", "ser", "ter", "ir",
            "voce", "meu", "minha", "seu", "sua", "tem", "sao",
            "quero", "preciso", "gostaria", "pode", "poderia", "favor"
    );

    private static final int MAX_HISTORICO = 20;

    private static final Pattern PRODUTO_ID_PATTERN = Pattern.compile("\\[PRODUTO_ID:(\\d+)]");

    public ChatbotRespostaDTO responder(ChatbotMensagemRequestDTO request) {
        List<Compra> historicoCompras = buscarHistoricoCompras();

        List<Produto> produtos = buscarProdutosRelevantes(request.getMensagem(), historicoCompras);

        String systemPrompt = construirSystemPrompt(produtos, historicoCompras);

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(maxTokens)
                .system(systemPrompt);

        if (request.getHistorico() != null) {
            List<ChatbotMensagemHistoricoDTO> historico = request.getHistorico();
            if (historico.size() > MAX_HISTORICO) {
                historico = historico.subList(historico.size() - MAX_HISTORICO, historico.size());
            }
            for (ChatbotMensagemHistoricoDTO msg : historico) {
                if ("usuario".equals(msg.getPapel())) {
                    paramsBuilder.addUserMessage(msg.getConteudo());
                } else {
                    paramsBuilder.addAssistantMessage(msg.getConteudo());
                }
            }
        }

        paramsBuilder.addUserMessage(request.getMensagem());

        Message message = anthropicClient.messages().create(paramsBuilder.build());

        String respostaTexto = message.content().stream()
                .filter(block -> block.isText())
                .map(block -> block.asText().text())
                .collect(Collectors.joining());

        Map<Long, Produto> produtoMap = produtos.stream()
                .collect(Collectors.toMap(Produto::getId, p -> p));

        List<ChatbotProdutoDTO> produtosReferenciados = extrairProdutosReferenciados(respostaTexto, produtoMap);

        String respostaLimpa = PRODUTO_ID_PATTERN.matcher(respostaTexto).replaceAll("");

        return ChatbotRespostaDTO.builder()
                .resposta(respostaLimpa.trim())
                .produtosReferenciados(produtosReferenciados)
                .build();
    }

    private List<Produto> buscarProdutosRelevantes(String mensagem, List<Compra> historicoCompras) {
        List<String> keywords = extrairPalavrasChave(mensagem);

        List<Produto> resultadosKeyword = new ArrayList<>();
        for (String keyword : keywords) {
            List<Produto> encontrados = produtoRepository.findAll(
                    ProdutoSpecifications.comFiltros(
                            keyword, keyword, keyword,
                            null, null, null, true,
                            null, null, null, null
                    )
            );
            resultadosKeyword.addAll(encontrados);
        }

        List<Produto> resultadosHistorico = new ArrayList<>();
        if (!historicoCompras.isEmpty()) {
            Set<Long> categoriasCompradas = historicoCompras.stream()
                    .filter(c -> c.getItens() != null)
                    .flatMap(c -> c.getItens().stream())
                    .filter(i -> i.getProduto() != null && i.getProduto().getCategorias() != null)
                    .flatMap(i -> i.getProduto().getCategorias().stream())
                    .map(Categoria::getId)
                    .collect(Collectors.toSet());

            for (Long categoriaId : categoriasCompradas) {
                List<Produto> porCategoria = produtoRepository.findAll(
                        ProdutoSpecifications.comFiltros(
                                null, null, null, null,
                                categoriaId, null, true,
                                null, null, null, null
                        )
                );
                resultadosHistorico.addAll(porCategoria);
            }
        }

        // prioriza primeiro produtos do historico, depois por keyword
        List<Produto> combinados = new ArrayList<>(resultadosHistorico);
        combinados.addAll(resultadosKeyword);

        List<Produto> unicos = combinados.stream()
                .collect(Collectors.toMap(Produto::getId, p -> p, (a, b) -> a))
                .values().stream()
                .limit(20)
                .collect(Collectors.toList());

        // fallback: so carrega tudo se nao tem historico NEM keywords
        if (unicos.isEmpty()) {
            unicos = produtoRepository.findAll(
                    ProdutoSpecifications.comFiltros(
                            null, null, null,
                            null, null, null, true,
                            null, null, null, null
                    )
            ).stream().limit(20).collect(Collectors.toList());
        }

        return unicos;
    }

    private List<String> extrairPalavrasChave(String mensagem) {
        return Arrays.stream(mensagem.toLowerCase().split("\\s+"))
                .map(w -> w.replaceAll("[^a-záàâãéèêíïóôõúüç]", ""))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Compra> buscarHistoricoCompras() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Collections.emptyList();
            }
            Cliente cliente = usuarioService.getClienteLogado();
            return compraRepository.findByClienteId(cliente.getId());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String construirSystemPrompt(List<Produto> produtos, List<Compra> historicoCompras) {
        StringBuilder sb = new StringBuilder();

        sb.append("Voce e o assistente virtual da Technomade, uma loja online de gadgets e equipamentos para nomades digitais.\n\n");
        sb.append("REGRAS OBRIGATORIAS:\n");
        sb.append("1. Voce so pode recomendar produtos da lista abaixo. NAO invente produtos que nao estao na lista.\n");
        sb.append("2. Se nenhum produto da lista atende ao pedido do usuario, diga honestamente que nao encontrou produtos adequados no catalogo.\n");
        sb.append("3. Ao mencionar um produto, SEMPRE inclua a referencia no formato [PRODUTO_ID:X] onde X e o id do produto.\n");
        sb.append("4. Responda sempre em portugues brasileiro.\n");
        sb.append("5. Seja conciso e util. Maximo 3-4 paragrafos.\n");
        sb.append("6. Nunca invente precos, descricoes ou marcas. Use EXATAMENTE os dados fornecidos.\n");
        sb.append("7. Se a pergunta nao tem relacao com os produtos da Technomade, responda apenas: \"Desculpe, nao posso te ajudar com isso. Posso te ajudar a encontrar algum produto da Technomade?\" e nada mais. Nao tente redirecionar, nao sugira produtos alternativos, nao explique o que voce faz.\n\n");

        sb.append("PRODUTOS DISPONIVEIS:\n");
        for (Produto p : produtos) {
            sb.append("---\n");
            sb.append("ID: ").append(p.getId());
            sb.append(" | Nome: ").append(p.getNome());
            sb.append(" | Marca: ").append(p.getMarca());
            sb.append(" | Preco: R$ ").append(String.format("%.2f", p.getValorVenda()));
            sb.append("\nDescricao: ").append(p.getDescricao());
            if (p.getCategorias() != null && !p.getCategorias().isEmpty()) {
                String cats = p.getCategorias().stream()
                        .map(Categoria::getNome)
                        .collect(Collectors.joining(", "));
                sb.append("\nCategorias: ").append(cats);
            }
            sb.append("\n");
        }
        sb.append("---\n");

        if (!historicoCompras.isEmpty()) {
            sb.append("\nHISTORICO DE COMPRAS DO CLIENTE:\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Compra compra : historicoCompras) {
                if (compra.getItens() != null) {
                    compra.getItens().forEach(item -> {
                        sb.append("- ").append(item.getProduto().getNome());
                        sb.append(" (comprado em ").append(compra.getDataCompra().format(fmt)).append(")\n");
                    });
                }
            }
            sb.append("\nUse o historico de compras para personalizar suas recomendacoes. ");
            sb.append("Voce DEVE recomendar produtos da MESMA CATEGORIA dos que o cliente ja comprou. ");
            sb.append("Por exemplo, se o cliente ja comprou fones de ouvido, recomende APENAS outros fones de ouvido que ele AINDA NAO TEM. ");
            sb.append("NAO recomende produtos de categorias diferentes (como notebooks, teclados, mochilas) a menos que o cliente peca explicitamente.\n");
        }

        return sb.toString();
    }

    private List<ChatbotProdutoDTO> extrairProdutosReferenciados(String resposta, Map<Long, Produto> produtoMap) {
        Set<Long> idsReferenciados = new LinkedHashSet<>();
        Matcher matcher = PRODUTO_ID_PATTERN.matcher(resposta);

        while (matcher.find()) {
            Long id = Long.parseLong(matcher.group(1));
            if (produtoMap.containsKey(id)) {
                idsReferenciados.add(id);
            }
        }

        return idsReferenciados.stream()
                .map(produtoMap::get)
                .map(this::toProdutoDTO)
                .collect(Collectors.toList());
    }

    private ChatbotProdutoDTO toProdutoDTO(Produto produto) {
        List<String> categorias = produto.getCategorias() != null
                ? produto.getCategorias().stream().map(Categoria::getNome).collect(Collectors.toList())
                : Collections.emptyList();

        return ChatbotProdutoDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .marca(produto.getMarca())
                .imagemUrl(produto.getImagemUrl())
                .valorVenda(produto.getValorVenda())
                .categorias(categorias)
                .build();
    }
}
