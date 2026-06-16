package br.com.technomade.ecommerce.repository.spec;

import br.com.technomade.ecommerce.model.Categoria;
import br.com.technomade.ecommerce.model.GrupoPrecificacao;
import br.com.technomade.ecommerce.model.Produto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProdutoSpecifications {

    public static Specification<Produto> comFiltros(
            String nome,
            String descricao,
            String marca,
            String codigoBarras,
            Long categoriaId,
            Long grupoPrecificacaoId,
            Boolean ativo,
            Double valorVendaMin,
            Double valorVendaMax,
            Double valorCustoMin,
            Double valorCustoMax
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }

            if (descricao != null && !descricao.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%"));
            }

            if (marca != null && !marca.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("marca")), "%" + marca.toLowerCase() + "%"));
            }

            if (codigoBarras != null && !codigoBarras.isBlank()) {
                predicates.add(cb.equal(root.get("codigoBarras"), codigoBarras));
            }

            if (categoriaId != null) {
                Join<Produto, Categoria> joinCategoria = root.join("categorias", JoinType.INNER);
                predicates.add(cb.equal(joinCategoria.get("id"), categoriaId));
                query.distinct(true);
            }

            if (grupoPrecificacaoId != null) {
                Join<Produto, GrupoPrecificacao> joinGrupo = root.join("grupoPrecificacao", JoinType.INNER);
                predicates.add(cb.equal(joinGrupo.get("id"), grupoPrecificacaoId));
            }

            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            if (valorVendaMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("valorVenda"), valorVendaMin));
            }

            if (valorVendaMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("valorVenda"), valorVendaMax));
            }

            if (valorCustoMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("valorCusto"), valorCustoMin));
            }

            if (valorCustoMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("valorCusto"), valorCustoMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
