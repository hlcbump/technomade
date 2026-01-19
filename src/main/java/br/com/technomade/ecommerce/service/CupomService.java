package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.Cupom;
import br.com.technomade.ecommerce.repository.CupomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CupomService {

    @Autowired
    private CupomRepository cupomRepository;

    public Cupom validarCupom(String codigo){
        Cupom cupom = cupomRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Cupom inválido: " + codigo));

        if (cupom.isUsado()){
            throw new IllegalStateException("Cupom já utilizado: " + codigo);
        }

        if (cupom.getValidade() != null && cupom.getValidade().isBefore(LocalDateTime.now())){
            throw new IllegalStateException("Cupom expirado: " + codigo);
        }

        return cupom;
    }

    public void marcarComoUsado(Cupom cupom){
        cupom.setUsado(true);
        cupomRepository.save(cupom);
    }
}
