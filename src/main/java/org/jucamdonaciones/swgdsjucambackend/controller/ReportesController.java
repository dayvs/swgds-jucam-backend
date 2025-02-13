package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.jucamdonaciones.swgdsjucambackend.repository.DonacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reportes")
public class ReportesController {

    @Autowired
    private DonacionRepository donacionRepository;

    @GetMapping("/donaciones")
    public List<Donacion> getDonaciones() {
        return donacionRepository.findAll();
    }
}