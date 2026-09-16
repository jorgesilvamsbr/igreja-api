package com.igreja.api.controller;

import com.igreja.api.model.Evento;
import com.igreja.api.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "*")
public class EventoController {

    @Autowired
    private EventoRepository repository;

    @GetMapping
    public List<Evento> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Evento criar(@RequestBody Evento evento) {
        return repository.save(evento);
    }

    @PutMapping("/{id}")
    public Evento atualizar(@PathVariable Long id, @RequestBody Evento eventoAtualizado) {
        return repository.findById(id)
            .map(evento -> {
                evento.setTitulo(eventoAtualizado.getTitulo());
                evento.setDataEvento(eventoAtualizado.getDataEvento());
                evento.setHorario(eventoAtualizado.getHorario());
                evento.setCategoria(eventoAtualizado.getCategoria());
                evento.setDescricao(eventoAtualizado.getDescricao());
                return repository.save(evento);
            })
            .orElseThrow(() -> new RuntimeException("Evento não encontrado com o id: " + id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
