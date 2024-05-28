package upc.edu.chatbotIA.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Appointment;
import upc.edu.chatbotIA.model.ServiceProduct;
import upc.edu.chatbotIA.model.Ticket;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SheetsService {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String SPREADSHEET_ID = "16HlW2_3ipWkywHkTvhI-nwEFhMOEJSEOVStT3rTNXv0";
    private static final String USERS_RANGE = "DB_USURIOS!A1:F4";
    private final Sheets sheetsService;

    public SheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = SheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public List<List<Object>> connectToGoogleSheets(String range) throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            return Collections.emptyList();
        }
        return values;
    }

    public void writeTicketToGoogleSheets(Ticket ticket) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Obtener la última fila con datos en la hoja de cálculo
        String range = "DB_TICKETS";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();
        int lastRow = response.getValues() != null ? response.getValues().size() + 1 : 1;

        // Construir el rango de la siguiente fila vacía
        String appendRange = "DB_TICKETS!A" + lastRow + ":G" + lastRow;

        // Formatear las fechas
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        String createdAtFormatted = ticket.getCreatedAt().format(formatter);
        String updatedAtFormatted = ticket.getUpdatedAt().format(formatter);

        List<List<Object>> values = Arrays.asList(
                Arrays.asList(
                        ticket.getId(),
                        ticket.getSenderId(),
                        ticket.getDescription(),
                        ticket.getUrgency(),
                        ticket.getStatus(),
                        createdAtFormatted,
                        updatedAtFormatted
                )
        );
        ValueRange body = new ValueRange().setValues(values);

        // Usar el método append para agregar los datos en la siguiente fila vacía
        AppendValuesResponse result = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, appendRange, body)
                .setValueInputOption("RAW")
                .execute();

        System.out.printf("%d cells appended.", result.getUpdates().getUpdatedCells());
    }

    public List<Ticket> getTicketsBySenderId(String senderId) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Obtener la última fila con datos en la hoja de cálculo
        String range = "DB_TICKETS";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            return Collections.emptyList();
        }

        // Determinar el rango dinámico basado en la cantidad de filas
        int lastRow = values.size();
        String dynamicRange = "DB_TICKETS!A1:G" + lastRow;

        // Obtener los datos del rango dinámico
        ValueRange dynamicResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, dynamicRange)
                .execute();

        List<List<Object>> dynamicValues = dynamicResponse.getValues();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        return dynamicValues.stream()
                .skip(1)
                .filter(row -> row.size() >= 7 && senderId.equals(row.get(1).toString()))
                .map(row -> {
                    String ticketId = row.get(0).toString();
                    String ticketSenderId = row.get(1).toString();
                    String description = row.get(2).toString();
                    String urgency = row.get(3).toString();
                    String status = row.get(4).toString();
                    LocalDateTime createdAt = LocalDateTime.parse(row.get(5).toString(), formatter);
                    LocalDateTime updatedAt = LocalDateTime.parse(row.get(6).toString(), formatter);
                    return new Ticket(ticketId, ticketSenderId, description, urgency, status, createdAt, updatedAt);
                })
                .collect(Collectors.toList());
    }

    public List<ServiceProduct> getServiciosByRucAndEstado(String ruc) throws IOException, GeneralSecurityException {
        System.out.println("RUC: " + ruc);
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Obtener la última fila con datos en la hoja de cálculo
        String range = "DB_EMPRESA_SERVICIOS";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            return Collections.emptyList();
        }

        // Determinar el rango dinámico basado en la cantidad de filas
        int lastRow = values.size();
        String dynamicRange = "DB_EMPRESA_SERVICIOS!A1:G" + lastRow;

        // Obtener los datos del rango dinámico
        ValueRange dynamicResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, dynamicRange)
                .execute();

        List<List<Object>> dynamicValues = dynamicResponse.getValues();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        return dynamicValues.stream()
                .skip(1)
                .filter(row -> row.size() >= 6 && ruc.equals(row.get(0).toString()) && ("ACTIVO".equalsIgnoreCase(row.get(2).toString()) || "EN PROCESO".equalsIgnoreCase(row.get(2).toString())))
                .map(row -> {
                    String Ruc = row.get(0).toString();
                    String servicio = row.get(1).toString();
                    String servicioEstado = row.get(2).toString();
                    LocalDateTime fechaPago = row.size() > 3 && !row.get(3).toString().isEmpty() ? LocalDateTime.parse(row.get(3).toString(), formatter) : null;
                    String velocidad = row.size() > 4 && !row.get(4).toString().isEmpty() ? row.get(4).toString() : "No corresponde para este servicio";
                    String precio = row.size() > 5 && !row.get(5).toString().isEmpty() ? row.get(5).toString() : "No corresponde para este servicio";
                    return new ServiceProduct(Ruc, servicio, servicioEstado, fechaPago, velocidad, precio);
                })
                .collect(Collectors.toList());
    }
    public List<Appointment> getAvailableAppointments(String ruc) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Obtener todas las filas de la hoja de cálculo
        String range = "DB_CITA_INST";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            return Collections.emptyList();
        }

        // Determinar el rango dinámico basado en la cantidad de filas
        int lastRow = values.size();
        String dynamicRange = "DB_CITA_INST!A1:D" + lastRow;

        // Obtener los datos del rango dinámico
        ValueRange dynamicResponse = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, dynamicRange)
                .execute();

        List<List<Object>> dynamicValues = dynamicResponse.getValues();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();

        // Filtrar manualmente los datos para obtener las citas disponibles para el RUC especificado
        List<Appointment> availableAppointments = new ArrayList<>();
        for (List<Object> row : dynamicValues) {
            if (row.size() >= 3) {
                String appointmentRuc = row.get(0).toString().trim();
                if (ruc.equals(appointmentRuc)) {
                    String service = row.get(1).toString();
                    LocalDateTime visitDate = LocalDateTime.parse(row.get(2).toString(), formatter);
                    String observation = row.size() >= 4 ? row.get(3).toString() : ""; // Verifica si hay observación o establece una cadena vacía
                    Appointment appointment = new Appointment(ruc, service, visitDate, observation);
                    if (appointment.getVisitDate().isAfter(now)) {
                        availableAppointments.add(appointment);
                    }
                }
            }
        }

        return availableAppointments;
    }

    public List<LocalDateTime> checkAvailableDates() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String sheetName = "DB_CITA_INST";
        String range = sheetName + "!C:C"; // Solo la tercera columna

        // Obtener todas las fechas de la tercera columna de la hoja de cálculo
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<Object> values = response.getValues().stream()
                .skip(1) // Saltar la primera fila (cabecera)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Filtrar y mapear las fechas válidas dentro del horario laboral y con citas disponibles
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        LocalDate now = LocalDate.now();
        Map<LocalDate, List<Integer>> dateAppointments = new HashMap<>();
        List<LocalDateTime> availableDates = new ArrayList<>();

        // Registrar las citas existentes en un mapa
        for (Object value : values) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value.toString(), formatter);
                if (!dateTime.toLocalDate().isBefore(now)) {
                    LocalDate date = dateTime.toLocalDate();
                    int hour = dateTime.getHour();

                    // Registrar solo las horas específicas
                    if (hour == 9 || hour == 12 || hour == 16) {
                        dateAppointments.computeIfAbsent(date, k -> new ArrayList<>()).add(hour);
                    }
                }
            } catch (DateTimeParseException e) {
                // Ignorar valores que no se puedan analizar como fechas
            }
        }

        // Generar las fechas disponibles según las condiciones
        for (int i = 0; i < 5; i++) { // Verificar las próximas 5 fechas
            LocalDate date = now.plusDays(i);
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue; // Ignorar sábados y domingos
            }

            List<Integer> appointments = dateAppointments.getOrDefault(date, new ArrayList<>());
            if (appointments.size() < 3) { // Solo considerar días con menos de 3 citas
                if (!appointments.contains(9)) {
                    availableDates.add(date.atTime(9, 0));
                }
                if (!appointments.contains(12)) {
                    availableDates.add(date.atTime(12, 0));
                }
                if (!appointments.contains(16)) {
                    availableDates.add(date.atTime(16, 0));
                }
            }
        }

        return availableDates;
    }


    public void scheduleAppointment(String ruc, String service, LocalDateTime visitDate, String observation) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);
        String visitDateFormatted = visitDate.format(formatter);

        List<List<Object>> values = Arrays.asList(
                Arrays.asList(
                        ruc,
                        service,
                        visitDateFormatted,
                        observation
                )
        );
        ValueRange body = new ValueRange().setValues(values);

        // Usar el método append para agregar los datos en la siguiente fila vacía
        AppendValuesResponse result = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, "DB_CITA_INST", body)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute();

        System.out.printf("%d cells appended.", result.getUpdates().getUpdatedCells());
    }



}