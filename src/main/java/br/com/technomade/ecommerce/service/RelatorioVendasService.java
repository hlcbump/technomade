package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.relatorio.ProdutoVendaDTO;
import br.com.technomade.ecommerce.model.ItemCompra;
import br.com.technomade.ecommerce.repository.ItemCompraRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioVendasService {

    @Autowired
    private ItemCompraRepository itemCompraRepository;

    @Transactional
    public List<ProdutoVendaDTO> gerarRelatorioPorPeriodo(LocalDate dataInicio, LocalDate dataFim){
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<ItemCompra> itens = itemCompraRepository.findByCompra_DataCompraBetween(inicio, fim);

        Map<String, List<ItemCompra>> agrupadoPorProduto = itens.stream()
                .collect(Collectors.groupingBy(item -> item.getProduto().getNome()));

        return agrupadoPorProduto.entrySet().stream()
                .map(entry -> {
                    String nomeProduto = entry.getKey();
                    List<ItemCompra> lista = entry.getValue();
                    long totalQuantidade = lista.stream()
                            .mapToLong(ItemCompra::getQuantidade)
                            .sum();
                    double totalValor = lista.stream()
                            .mapToDouble(i -> i.getPrecoUnitario() * i.getQuantidade())
                            .sum();
                    return new ProdutoVendaDTO(nomeProduto, totalQuantidade, totalValor);
                })
                .collect(Collectors.toList());
    }
}
