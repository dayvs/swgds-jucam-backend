package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.jucamdonaciones.swgdsjucambackend.repository.DonacionRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.SuscripcionRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.SuscriptorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Optional;

@RestController
@RequestMapping("/reportes")
public class ReportesController {

    @Autowired
    private DonacionRepository donacionRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private SuscriptorRepository suscriptorRepository;

    /**
     * Endpoint para obtener los datos del dashboard de reportes.
     * Parámetros (opcionales):
     * - startDate: fecha de inicio en formato ISO (yyyy-MM-dd'T'HH:mm:ss)
     * - endDate: fecha fin en formato ISO (yyyy-MM-dd'T'HH:mm:ss)
     * - includeDonaciones: (boolean) si se incluyen las donaciones (default true)
     * - includeSuscripciones: (boolean) si se incluyen los pagos de suscripciones (default true)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardReport(
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
         @RequestParam(defaultValue = "true") boolean includeDonaciones,
         @RequestParam(defaultValue = "true") boolean includeSuscripciones
    ) {
         // Si no se especifican fechas, usar desde el primer día del mes actual hasta el momento actual.
         if (startDate == null || endDate == null) {
             LocalDate today = LocalDate.now();
             startDate = today.withDayOfMonth(1).atStartOfDay();
             endDate = LocalDateTime.now();
         }

         // Obtener todas las donaciones dentro del rango (incluye pagos de suscripciones)
         List<Donacion> donacionesTotales = donacionRepository.findByFechaDonacionBetween(startDate, endDate);

         // Separar en donaciones y pagos de suscripciones
         List<Donacion> listaDonaciones = new ArrayList<>();
         List<Donacion> listaSuscripciones = new ArrayList<>();

         for (Donacion d : donacionesTotales) {
             if (d.getSuscripcionId() == null) {
                 listaDonaciones.add(d);
             } else {
                 listaSuscripciones.add(d);
             }
         }

         if (!includeDonaciones) {
             listaDonaciones.clear();
         }
         if (!includeSuscripciones) {
             listaSuscripciones.clear();
         }

         BigDecimal totalDonaciones = listaDonaciones.stream()
             .map(Donacion::getMonto)
             .reduce(BigDecimal.ZERO, BigDecimal::add);

         BigDecimal totalSuscripciones = listaSuscripciones.stream()
             .map(Donacion::getMonto)
             .reduce(BigDecimal.ZERO, BigDecimal::add);

         BigDecimal totalRecaudado = totalDonaciones.add(totalSuscripciones);

         // Calcular porcentajes para el donut chart
         double porcentajeDonaciones = 0;
         double porcentajeSuscripciones = 0;
         if (totalRecaudado.compareTo(BigDecimal.ZERO) > 0) {
             porcentajeDonaciones = totalDonaciones.multiply(BigDecimal.valueOf(100))
                     .divide(totalRecaudado, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
             porcentajeSuscripciones = totalSuscripciones.multiply(BigDecimal.valueOf(100))
                     .divide(totalRecaudado, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
         }

         // Preparar datos para la tabla de donaciones
         List<Map<String, Object>> tablaDonaciones = new ArrayList<>();
         DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
         for (Donacion d : listaDonaciones) {
             Map<String, Object> registro = new HashMap<>();
             registro.put("fecha", d.getFechaDonacion().format(dtf));
             if (d.getDonador() != null) {
                 registro.put("donador", d.getDonador().getNombre());
                 registro.put("correo", d.getDonador().getEmail());
                 registro.put("telefono", d.getDonador().getTelefono());
             } else {
                 registro.put("donador", "Desconocido");
                 registro.put("correo", "");
                 registro.put("telefono", "");
             }
             registro.put("monto", d.getMonto());
             tablaDonaciones.add(registro);
         }

         // Preparar datos para la tabla de suscripciones
         List<Map<String, Object>> tablaSuscripciones = new ArrayList<>();
         for (Donacion d : listaSuscripciones) {
             Map<String, Object> registro = new HashMap<>();
             registro.put("fecha", d.getFechaDonacion().format(dtf));
             // Buscar la suscripción asociada
             Optional<org.jucamdonaciones.swgdsjucambackend.model.Suscripcion> optSuscripcion =
                     suscripcionRepository.findById(d.getSuscripcionId());
             if (optSuscripcion.isPresent()) {
                 org.jucamdonaciones.swgdsjucambackend.model.Suscripcion suscripcion = optSuscripcion.get();
                 registro.put("suscriptor", suscripcion.getSuscriptor().getNombre());
                 registro.put("correo", suscripcion.getSuscriptor().getEmail());
                 registro.put("telefono", suscripcion.getSuscriptor().getTelefono());
                 // Para el plan, se utiliza el servicioId (se puede adaptar si se tiene nombre de plan)
                 registro.put("plan", suscripcion.getServicioId().toString());
             } else {
                 registro.put("suscriptor", "Desconocido");
                 registro.put("correo", "");
                 registro.put("telefono", "");
                 registro.put("plan", "");
             }
             registro.put("monto", d.getMonto());
             tablaSuscripciones.add(registro);
         }

         // Datos para el donut chart
         Map<String, Object> donutChart = new HashMap<>();
         donutChart.put("donaciones", Map.of("monto", totalDonaciones, "porcentaje", porcentajeDonaciones));
         donutChart.put("suscripciones", Map.of("monto", totalSuscripciones, "porcentaje", porcentajeSuscripciones));

         // Preparar datos para el pie chart de suscripciones (agrupado por plan)
         Map<String, BigDecimal> pieChartMap = new HashMap<>();
         for (Donacion d : listaSuscripciones) {
             Optional<org.jucamdonaciones.swgdsjucambackend.model.Suscripcion> optSuscripcion =
                     suscripcionRepository.findById(d.getSuscripcionId());
             if (optSuscripcion.isPresent()) {
                 org.jucamdonaciones.swgdsjucambackend.model.Suscripcion suscripcion = optSuscripcion.get();
                 String plan = suscripcion.getServicioId().toString();
                 pieChartMap.put(plan, pieChartMap.getOrDefault(plan, BigDecimal.ZERO).add(d.getMonto()));
             }
         }
         List<Map<String, Object>> pieChart = new ArrayList<>();
         for (Map.Entry<String, BigDecimal> entry : pieChartMap.entrySet()) {
             double porcentaje = 0;
             if (totalSuscripciones.compareTo(BigDecimal.ZERO) > 0) {
                 porcentaje = entry.getValue().multiply(BigDecimal.valueOf(100))
                         .divide(totalSuscripciones, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
             }
             Map<String, Object> data = new HashMap<>();
             data.put("plan", entry.getKey());
             data.put("monto", entry.getValue());
             data.put("porcentaje", porcentaje);
             pieChart.add(data);
         }

         // Construir la respuesta final
         Map<String, Object> response = new HashMap<>();
         response.put("totalRecaudado", totalRecaudado);
         response.put("donutChart", donutChart);
         response.put("tablaDonaciones", tablaDonaciones);
         response.put("tablaSuscripciones", tablaSuscripciones);
         response.put("pieChart", pieChart);

         return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para descargar el reporte en formato CSV.
     * Los parámetros son los mismos que para el dashboard.
     */
    @GetMapping("/csv")
    public ResponseEntity<?> downloadCSVReport(
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
         @RequestParam(defaultValue = "true") boolean includeDonaciones,
         @RequestParam(defaultValue = "true") boolean includeSuscripciones
    ) {
         if (startDate == null || endDate == null) {
             LocalDate today = LocalDate.now();
             startDate = today.withDayOfMonth(1).atStartOfDay();
             endDate = LocalDateTime.now();
         }

         List<Donacion> donacionesTotales = donacionRepository.findByFechaDonacionBetween(startDate, endDate);

         List<Donacion> listaDonaciones = new ArrayList<>();
         List<Donacion> listaSuscripciones = new ArrayList<>();

         for (Donacion d : donacionesTotales) {
             if (d.getSuscripcionId() == null) {
                 listaDonaciones.add(d);
             } else {
                 listaSuscripciones.add(d);
             }
         }
         if (!includeDonaciones) {
             listaDonaciones.clear();
         }
         if (!includeSuscripciones) {
             listaSuscripciones.clear();
         }

         // Construir el contenido CSV
         StringBuilder csvBuilder = new StringBuilder();
         csvBuilder.append("Fecha,Nombre,Correo,Teléfono,Monto,Tipo,Plan\n");

         DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

         // Procesar donaciones
         for (Donacion d : listaDonaciones) {
             String fecha = d.getFechaDonacion().format(dtf);
             String nombre = d.getDonador() != null ? d.getDonador().getNombre() : "";
             String correo = d.getDonador() != null ? d.getDonador().getEmail() : "";
             String telefono = d.getDonador() != null ? d.getDonador().getTelefono() : "";
             String monto = d.getMonto().toString();
             csvBuilder.append(String.format("%s,%s,%s,%s,%s,Donación,\n", fecha, nombre, correo, telefono, monto));
         }

         // Procesar suscripciones
         for (Donacion d : listaSuscripciones) {
             String fecha = d.getFechaDonacion().format(dtf);
             String nombre = "";
             String correo = "";
             String telefono = "";
             String plan = "";
             Optional<org.jucamdonaciones.swgdsjucambackend.model.Suscripcion> optSuscripcion =
                     suscripcionRepository.findById(d.getSuscripcionId());
             if (optSuscripcion.isPresent()) {
                 org.jucamdonaciones.swgdsjucambackend.model.Suscripcion suscripcion = optSuscripcion.get();
                 nombre = suscripcion.getSuscriptor().getNombre();
                 correo = suscripcion.getSuscriptor().getEmail();
                 telefono = suscripcion.getSuscriptor().getTelefono();
                 plan = suscripcion.getServicioId().toString();
             }
             String monto = d.getMonto().toString();
             csvBuilder.append(String.format("%s,%s,%s,%s,%s,Suscripción,%s\n", fecha, nombre, correo, telefono, monto, plan));
         }

         byte[] csvBytes = csvBuilder.toString().getBytes();
         ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(csvBytes);

         // Generar nombre de archivo con timestamp
         String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy_HHmmss"));
         String filename = "Reporte_" + timestamp + "_Jucam.csv";

         HttpHeaders headers = new HttpHeaders();
         headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
         headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

         return ResponseEntity.ok()
             .headers(headers)
             .body(new InputStreamResource(byteArrayInputStream));
    }
}