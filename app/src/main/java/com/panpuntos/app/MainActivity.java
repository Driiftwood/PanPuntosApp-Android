package com.panpuntos.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etNit;
    private Button btnConsultar;
    private MaterialCardView cardResultado;
    private TextView tvNombreCliente, tvNitCliente, tvPuntos, tvEstadoRecompensas;

    private static final String IP_SERVIDOR = "172.16.30.104";
    private static final String PUERTO = "80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNit = findViewById(R.id.etNit);
        btnConsultar = findViewById(R.id.btnConsultar);
        cardResultado = findViewById(R.id.cardResultado);
        tvNombreCliente = findViewById(R.id.tvNombreCliente);
        tvNitCliente = findViewById(R.id.tvNitCliente);
        tvPuntos = findViewById(R.id.tvPuntos);
        tvEstadoRecompensas = findViewById(R.id.tvEstadoRecompensas);

        btnConsultar.setOnClickListener(v -> consultarPuntosServer());
    }

    private void consultarPuntosServer() {
        String nit = etNit.getText().toString().trim();
        if (nit.isEmpty()) {
            Toast.makeText(this, "Ingrese un NIT o DPI válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://" + IP_SERVIDOR + ":" + PUERTO + "/api/puntos?nit=" + nit;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String nombre = response.getString("nombre");
                        String nitResp = response.getString("nit");
                        int puntos = response.getInt("puntos");

                        tvNombreCliente.setText(nombre);
                        tvNitCliente.setText("NIT / DPI: " + nitResp);
                        tvPuntos.setText(puntos + " PTS");

                        if (puntos >= 50) {
                            tvEstadoRecompensas.setText("🎉 ¡Felicidades! Puedes canjear un Menú Completo (50 pts)");
                        } else if (puntos >= 30) {
                            tvEstadoRecompensas.setText("🎁 Puedes canjear un Sándwich Individual (30 pts)");
                        } else if (puntos >= 10) {
                            tvEstadoRecompensas.setText("🥤 Puedes canjear una Bebida (10 pts)");
                        } else {
                            tvEstadoRecompensas.setText("Te faltan " + (10 - puntos) + " pts para tu primera recompensa.");
                        }

                        cardResultado.setVisibility(View.VISIBLE);

                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Error procesando los datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Cliente no encontrado o servidor apagado", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }
}