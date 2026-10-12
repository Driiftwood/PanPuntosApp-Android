package com.panpuntos.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etNit;
    private Button btnConsultar, btnCanjear;
    private MaterialCardView cardResultado;
    private TextView tvNombreCliente, tvNitCliente, tvPuntos, tvEstadoRecompensas;
    private ProgressBar pbCarga, pbMetaPuntos;

    private static final String IP_SERVIDOR = "172.16.30.104";
    private static final String PUERTO = "80";

    private int puntosActuales = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNit = findViewById(R.id.etNit);
        btnConsultar = findViewById(R.id.btnConsultar);
        btnCanjear = findViewById(R.id.btnCanjear);
        cardResultado = findViewById(R.id.cardResultado);
        tvNombreCliente = findViewById(R.id.tvNombreCliente);
        tvNitCliente = findViewById(R.id.tvNitCliente);
        tvPuntos = findViewById(R.id.tvPuntos);
        tvEstadoRecompensas = findViewById(R.id.tvEstadoRecompensas);
        pbCarga = findViewById(R.id.pbCarga);
        pbMetaPuntos = findViewById(R.id.pbMetaPuntos);

        btnConsultar.setOnClickListener(v -> consultarPuntosServer());
        btnCanjear.setOnClickListener(v -> prepararYCanjearPuntos());
    }

    private void consultarPuntosServer() {
        String nit = etNit.getText().toString().trim();
        if (nit.isEmpty()) {
            Toast.makeText(this, "Ingrese un NIT o DPI válido", Toast.LENGTH_SHORT).show();
            return;
        }

        pbCarga.setVisibility(View.VISIBLE);
        cardResultado.setVisibility(View.GONE);

        String url = "http://" + IP_SERVIDOR + ":" + PUERTO + "/api/puntos?nit=" + nit;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    pbCarga.setVisibility(View.GONE);

                    try {
                        // Extracción segura para evitar excepciones si faltan campos o hay caracteres especiales
                        String nombre = response.optString("nombre", "Cliente Registrado");
                        String nitResp = response.optString("nit", nit);
                        puntosActuales = response.optInt("puntos", 0);

                        // Si el nombre viene en codificación ISO o con caracteres dañados, forzamos UTF-8
                        try {
                            byte[] bytes = nombre.getBytes(StandardCharsets.ISO_8859_1);
                            nombre = new String(bytes, StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}

                        tvNombreCliente.setText(nombre);
                        tvNitCliente.setText("NIT / DPI: " + nitResp);
                        tvPuntos.setText(puntosActuales + " PTS");

                        pbMetaPuntos.setProgress(Math.min(puntosActuales, 50));

                        if (puntosActuales >= 50) {
                            tvEstadoRecompensas.setText("🎉 ¡Felicidades! Puedes canjear un Menú Completo (50 pts)");
                            btnCanjear.setVisibility(View.VISIBLE);
                        } else if (puntosActuales >= 30) {
                            tvEstadoRecompensas.setText("🎁 Puedes canjear un Sándwich Individual (30 pts)");
                            btnCanjear.setVisibility(View.VISIBLE);
                        } else if (puntosActuales >= 10) {
                            tvEstadoRecompensas.setText("🥤 Puedes canjear una Bebida (10 pts)");
                            btnCanjear.setVisibility(View.VISIBLE);
                        } else {
                            int faltantes = 10 - puntosActuales;
                            tvEstadoRecompensas.setText("Te faltan " + faltantes + " pts para tu primera recompensa.");
                            btnCanjear.setVisibility(View.GONE);
                        }

                        cardResultado.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error al procesar los datos del cliente", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    pbCarga.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Cliente no encontrado o servidor desconectado", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private void prepararYCanjearPuntos() {
        String nit = etNit.getText().toString().trim();

        int puntosADescontar = 0;
        if (puntosActuales >= 50) {
            puntosADescontar = 50;
        } else if (puntosActuales >= 30) {
            puntosADescontar = 30;
        } else if (puntosActuales >= 10) {
            puntosADescontar = 10;
        }

        if (puntosADescontar > 0) {
            procesarCanjeServer(nit, puntosADescontar);
        } else {
            Toast.makeText(this, "No tienes puntos suficientes para un canje", Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarCanjeServer(String nit, int puntosADescontar) {
        pbCarga.setVisibility(View.VISIBLE);

        String url = "http://" + IP_SERVIDOR + ":" + PUERTO + "/api/canjear";

        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        try {
            postData.put("nit", nit);
            postData.put("puntos", puntosADescontar);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    pbCarga.setVisibility(View.GONE);
                    try {
                        boolean exito = response.optBoolean("success", false);
                        if (exito) {
                            String codigo = response.optString("codigo", "PAN-000000");
                            mostrarDialogoCanjeExitoso(codigo, puntosADescontar);
                            consultarPuntosServer();
                        } else {
                            String mensaje = response.optString("message", "No se pudo procesar el canje");
                            Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Canje procesado.", Toast.LENGTH_SHORT).show();
                        consultarPuntosServer();
                    }
                },
                error -> {
                    pbCarga.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error de red al intentar realizar el canje", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private void mostrarDialogoCanjeExitoso(String codigoCanje, int puntosCanjeados) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 ¡CANJE EXITOSO!")
                .setMessage("Muestra este código al cajero para reclamar tu premio:\n\n" +
                        "CÓDIGO DE CUPÓN:\n" + codigoCanje + "\n\n" +
                        "Puntos canjeados: " + puntosCanjeados + " PTS")
                .setPositiveButton("ENTENDIDO", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}