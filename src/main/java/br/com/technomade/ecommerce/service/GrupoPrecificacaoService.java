package br.com.technomade.ecommerce.service;

import br.com.technomade.ecommerce.model.GrupoPrecificacao;
import br.com.technomade.ecommerce.repository.GrupoPrecificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GrupoPrecificacaoService {

    @Autowired
    private GrupoPrecificacaoRepository grupoPrecificacaoRepository;

    public GrupoPrecificacao salvar(GrupoPrecificacao grupoPrecificacao){
        return grupoPrecificacaoRepository.save(grupoPrecificacao);
    }

    public List<GrupoPrecificacao> listarTodos(){
        return grupoPrecificacaoRepository.findAll();
    }

    public Optional<GrupoPrecificacao> buscarPorId(Long id){
        return grupoPrecificacaoRepository.findById(id);
    }

    public void deletarPorId(Long id){
        grupoPrecificacaoRepository.deleteById(id);
    }
}
