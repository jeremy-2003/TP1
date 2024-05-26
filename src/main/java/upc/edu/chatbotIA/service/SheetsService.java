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
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Ticket;
import java.time.format.DateTimeFormatter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SheetsService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String SPREADSHEET_ID = "16HlW2_3ipWkywHkTvhI-nwEFhMOEJSEOVStT3rTNXv0";
    private static final String USERS_RANGE = "DB_USURIOS!A1:F4";
    public static List<List<Object>> connectToGoogleSheets() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, USERS_RANGE)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            return values;
        }
        return Collections.emptyList();
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
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
}