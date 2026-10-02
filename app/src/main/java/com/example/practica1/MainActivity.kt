package com.example.practica1

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppPractica1()
        }
    }
}

fun contarPrimos(limite: Int): Int {
    var total = 0
    for (n in 2..limite) {
        var esPrimo = true
        var d = 2
        while (d * d <= n) {
            if (n % d == 0) {
                esPrimo = false
                break
            }
            d++
        }
        if (esPrimo) total++
    }
    return total
}

// Actividad 3
class ArchivoFalso {
    private val ocupado = AtomicBoolean(false)
    val lineas = mutableListOf<String>()

    fun guardar(texto: String) {
        if (!ocupado.compareAndSet(false, true)) {
            throw Exception("Choque de hilos")
        }
        try {
            Thread.sleep(40) // simula tiempo de guardado
            lineas.add(texto)
        } finally {
            ocupado.set(false)
        }
    }
}

@Composable
fun AppPractica1() {
    var pantallaActual by remember { mutableIntStateOf(1) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { pantallaActual = 1 }) { Text("Parte 1") }
            Button(onClick = { pantallaActual = 2 }) { Text("Parte 2") }
            Button(onClick = { pantallaActual = 3 }) { Text("Parte 3") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pantallaActual == 1) {
            VistaUno()
        } else if (pantallaActual == 2) {
            VistaDos()
        } else {
            VistaTres()
        }
    }
}

@Composable
fun VistaUno() {
    val scope = rememberCoroutineScope()
    var textLimite by remember { mutableStateOf("3000000") }
    var resultado by remember { mutableStateOf("Esperando...") }
    var tiempo by remember { mutableStateOf(0L) }
    var toques by remember { mutableIntStateOf(0) }

    Column {
        Text("Límite de primos:")
        TextField(value = textLimite, onValueChange = { textLimite = it })

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            val lim = textLimite.toIntOrNull() ?: 3000000
            val inicio = SystemClock.elapsedRealtime()
            val res = contarPrimos(lim) // Esto congela la app
            tiempo = SystemClock.elapsedRealtime() - inicio
            resultado = "Primos encontrados: $res"
        }) {
            Text("Botón Secuencial (Se traba)")
        }

        Button(onClick = {
            val lim = textLimite.toIntOrNull() ?: 3000000
            resultado = "Calculando..."
            scope.launch {
                val inicio = SystemClock.elapsedRealtime()
                val res = withContext(Dispatchers.Default) { contarPrimos(lim) }
                tiempo = SystemClock.elapsedRealtime() - inicio
                resultado = "Primos encontrados: $res"
            }
        }) {
            Text("Botón con Corrutina (No se traba)")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(resultado)
        Text("Tiempo que tardó: $tiempo ms")

        Spacer(modifier = Modifier.height(30.dp))
        Text("Prueba de toques en pantalla:")
        Button(onClick = { toques++ }) {
            Text("Toques: $toques")
        }
    }
}

@Composable
fun VistaDos() {
    val scope = rememberCoroutineScope()
    var textN by remember { mutableStateOf("4") }
    var historial by remember { mutableStateOf("") }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Prueba de Tiempos N a 5N")
        TextField(value = textN, onValueChange = { textN = it }, label = { Text("Valor de N") })

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            historial = "Calculando...\n\n"
            val n = textN.toIntOrNull() ?: 4
            val lim = 150000

            scope.launch {
                for (k in 1..5) {
                    val repeticiones = n * k

                    // Tiempo secuencial
                    val t1 = SystemClock.elapsedRealtime()
                    withContext(Dispatchers.Default) {
                        for(i in 1..repeticiones) {
                            contarPrimos(lim)
                        }
                    }
                    val tiempoSec = SystemClock.elapsedRealtime() - t1


                    val t2 = SystemClock.elapsedRealtime()
                    withContext(Dispatchers.Default) {
                        val tareas = mutableListOf<Deferred<Int>>()
                        for(i in 1..repeticiones) {
                            tareas.add(async { contarPrimos(lim) })
                        }
                        tareas.awaitAll()
                    }
                    val tiempoConc = SystemClock.elapsedRealtime() - t2

                    historial += "Reps: $repeticiones | Sec: $tiempoSec ms | Conc: $tiempoConc ms\n"
                }
                historial += "\nFin de la prueba."
            }
        }) {
            Text("Iniciar")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(historial)
    }
}

@Composable
fun VistaTres() {
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    val hilosTotales = 8

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Problema del Archivo Compartido")

        Row {
            Button(onClick = {
                log.clear()
                log.add("Prueba SIN Mutex:")
                val archivo = ArchivoFalso()

                scope.launch {
                    val jobs = mutableListOf<Job>()
                    for(i in 1..hilosTotales) {
                        jobs.add(launch(Dispatchers.IO) {
                            try {
                                archivo.guardar("Texto del hilo $i")
                                withContext(Dispatchers.Main) { log.add("Hilo $i escribio bien") }
                            } catch(e: Exception) {
                                withContext(Dispatchers.Main) { log.add("Error en hilo $i") }
                            }
                        })
                    }
                    jobs.joinAll()
                    log.add("Resultados guardados: ${archivo.lineas.size} de $hilosTotales")
                }
            }) { Text("Sin Mutex") }

            Spacer(modifier = Modifier.width(10.dp))

            Button(onClick = {
                log.clear()
                log.add("Prueba CON Mutex:")
                val archivo = ArchivoFalso()
                val mutex = Mutex()

                scope.launch {
                    val jobs = mutableListOf<Job>()
                    for(i in 1..hilosTotales) {
                        jobs.add(launch(Dispatchers.IO) {
                            try {
                                mutex.withLock {
                                    archivo.guardar("Texto del hilo $i")
                                }
                                withContext(Dispatchers.Main) { log.add("Hilo $i escribio bien") }
                            } catch(e: Exception) {
                                withContext(Dispatchers.Main) { log.add("Error en hilo $i") }
                            }
                        })
                    }
                    jobs.joinAll()
                    log.add("Resultados guardados: ${archivo.lineas.size} de $hilosTotales")
                }
            }) { Text("Con Mutex") }
        }

        Spacer(modifier = Modifier.height(10.dp))

        log.forEach { linea ->
            Text(linea)
        }
        //el equipo 4 estuvo aqui
    }
}