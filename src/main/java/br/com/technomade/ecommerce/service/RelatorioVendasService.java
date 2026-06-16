package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.dto.relatorio.CategoriaVendaDTO;
import br.com.technomade.ecommerce.dto.relatorio.ProdutoVendaDTO;
import br.com.technomade.ecommerce.dto.relatorio.SerieTemporalCategoriaDTO;
import br.com.technomade.ecommerce.model.Categoria;
import br.com.technomade.ecommerce.model.ItemCompra;
import br.com.technomade.ecommerce.repository.ItemCompraRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    @Transactional
    public List<CategoriaVendaDTO> gerarRelatorioPorCategoria(LocalDate dataInicio, LocalDate dataFim){
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<ItemCompra> itens = itemCompraRepository.findByCompra_DataCompraBetween(inicio, fim);
        Map<String, CategoriaAcumulador> acumulado = new HashMap<>();

        for (ItemCompra item : itens){
            if (item.getProduto() == null || item.getProduto().getCategorias() == null){
                continue;
            }
            for (Categoria categoria : item.getProduto().getCategorias()){
                CategoriaAcumulador acc = acumulado.computeIfAbsent(
                        categoria.getNome(),
                        k -> new CategoriaAcumulador()
                );
                acc.totalQuantidade += item.getQuantidade();
                acc.totalValor += item.getPrecoUnitario() * item.getQuantidade();
            }
        }

        return acumulado.entrySet().stream()
                .map(entry -> new CategoriaVendaDTO(
                        entry.getKey(),
                        entry.getValue().totalQuantidade,
                        entry.getValue().totalValor
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public SerieTemporalCategoriaDTO gerarSerieTemporalPorCategoria(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<ItemCompra> itens = itemCompraRepository.findByCompra_DataCompraBetween(inicio, fim);

        List<YearMonth> meses = new ArrayList<>();
        YearMonth mesAtual = YearMonth.from(dataInicio);
        YearMonth mesFim = YearMonth.from(dataFim);
        while (!mesAtual.isAfter(mesFim)) {
            meses.add(mesAtual);
            mesAtual = mesAtual.plusMonths(1);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        List<String> periodos = meses.stream().map(m -> m.format(fmt)).collect(Collectors.toList());

        // categoria -> mes -> quantidade
        Map<String, Map<YearMonth, Long>> dados = new LinkedHashMap<>();

        for (ItemCompra item : itens) {
            if (item.getProduto() == null || item.getProduto().getCategorias() == null) {
                continue;
            }
            YearMonth mesItem = YearMonth.from(item.getCompra().getDataCompra());
            for (Categoria categoria : item.getProduto().getCategorias()) {
                dados.computeIfAbsent(categoria.getNome(), k -> new HashMap<>())
                        .merge(mesItem, (long) item.getQuantidade(), Long::sum);
            }
        }

        List<SerieTemporalCategoriaDTO.SerieCategoriaDTO> series = dados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Long> quantidades = meses.stream()
                            .map(m -> entry.getValue().getOrDefault(m, 0L))
                            .collect(Collectors.toList());
                    return SerieTemporalCategoriaDTO.SerieCategoriaDTO.builder()
                            .categoria(entry.getKey())
                            .quantidades(quantidades)
                            .build();
                })
                .collect(Collectors.toList());

        long totalCompras = itens.stream()
                .map(item -> item.getCompra().getId())
                .distinct()
                .count();

        return SerieTemporalCategoriaDTO.builder()
                .totalCompras(totalCompras)
                .periodos(periodos)
                .series(series)
                .build();
    }

    private static class CategoriaAcumulador {
        long totalQuantidade = 0;
        double totalValor = 0.0;
    }
}
