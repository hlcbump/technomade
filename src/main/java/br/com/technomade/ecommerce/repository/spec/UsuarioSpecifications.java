package br.com.technomade.ecommerce.repository.spec;

import br.com.technomade.ecommerce.model.Role;
import br.com.technomade.ecommerce.model.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// classe que monta filtros dinamicos
// só aplica os filtros que foram preenchidos
public class UsuarioSpecifications {

    // campos para montar a query
    public static Specification<Usuario> comFiltros(
            String nome,
            String email,
            Role role,
            Boolean ativo
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }

            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
