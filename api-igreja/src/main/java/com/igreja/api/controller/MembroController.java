package com.igreja.api.controller;

import com.igreja.api.model.Membro;
import com.igreja.api.repository.MembroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membros")
@CrossOrigin(origins = "*")
public class MembroController {

    @Autowired
    private MembroRepository repository;

    @GetMapping
    public List<Membro> listar(@RequestParam(required = false) String busca) {
        if (busca != null && !busca.isEmpty()) {
            return repository.findByNomeContainingIgnoreCase(busca);
        }
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Membro criar(@RequestBody Membro membro) {
        return repository.save(membro);
    }

    @PutMapping("/{id}")
    public Membro atualizar(@PathVariable Long id, @RequestBody Membro dados) {
        Membro membro = repository.findById(id).orElseThrow();
        membro.setNome(dados.getNome());
        membro.setGenero(dados.getGenero());
        membro.setDataNascimento(dados.getDataNascimento());
        membro.setDataBatismo(dados.getDataBatismo());
        membro.setInicioMembresia(dados.getInicioMembresia());
        membro.setFuncao(dados.getFuncao());
        membro.setTelefone(dados.getTelefone());
        membro.setStatus(dados.getStatus());
        return repository.save(membro);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}