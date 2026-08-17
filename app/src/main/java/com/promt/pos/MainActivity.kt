package com.promt.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.security.MessageDigest
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PromtPosApp() }
    }
}

enum class Role { ADMIN, EMPLOYEE }
class Employee(
    val id: String = UUID.randomUUID().toString(),
    val login: String,
    val passwordHash: String,
    val role: Role
)
data class HallTable(val id: String = UUID.randomUUID().toString(), val number: Int)
data class MenuItem(val id: String = UUID.randomUUID().toString(), val name: String, val price: Double, val category: String)
data class ReceiptItem(val menuItem: MenuItem, val qty: Int)
class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val tableId: String,
    val createdAt: Instant = Instant.now(),
) {
    val items = mutableStateListOf<ReceiptItem>()
    var discount by mutableStateOf(0)
    var cardPaid by mutableStateOf(0.0)
    var cashPaid by mutableStateOf(0.0)
    var closed by mutableStateOf(false)
    var timerStartedAt by mutableStateOf<Instant?>(null)
}

private val ALLOWED_DISCOUNTS = listOf(10, 20, 30)

private fun hashPassword(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

@Composable
fun PromtPosApp() {
    val employees = remember { mutableStateListOf<Employee>() }
    val tables = remember { mutableStateListOf(HallTable(number = 1), HallTable(number = 2), HallTable(number = 3)) }
    val menu = remember { mutableStateListOf(MenuItem(name = "Кальян классический", price = 1500.0, category = "Кальяны")) }
    val receipts = remember { mutableStateListOf<Receipt>() }

    var currentUser by remember { mutableStateOf<Employee?>(null) }

    if (currentUser == null) {
        LoginScreen(
            employees = employees,
            onLogin = { currentUser = it },
            onBootstrapAdmin = { login, password ->
                val admin = Employee(login = login, passwordHash = hashPassword(password), role = Role.ADMIN)
                employees.add(admin)
                currentUser = admin
            }
        )
    } else if (currentUser?.role == Role.ADMIN) {
        AdminScreen(employees, tables, menu) { currentUser = null }
    } else {
        EmployeeScreen(tables, menu, receipts) { currentUser = null }
    }
}

@Composable
fun LoginScreen(
    employees: List<Employee>,
    onLogin: (Employee) -> Unit,
    onBootstrapAdmin: (String, String) -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Promt POS", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            val user = employees.find { it.login == login && it.passwordHash == hashPassword(password) }
            if (user != null) onLogin(user) else error = "Неверные данные"
        }) { Text("Войти") }
        if (employees.isEmpty()) {
            Text("Сначала создайте администратора в админ-панели", color = Color.Gray)
            Button(onClick = {
                if (login.isNotBlank() && password.isNotBlank()) {
                    onBootstrapAdmin(login, password)
                } else {
                    error = "Введите логин и пароль для первого администратора"
                }
            }) { Text("Создать первого администратора") }
        }
        if (error.isNotBlank()) Text(error, color = Color.Red)
    }
}

@Composable
fun AdminScreen(
    employees: MutableList<Employee>,
    tables: MutableList<HallTable>,
    menu: MutableList<MenuItem>,
    onLogout: () -> Unit
) {
    var employeeLogin by remember { mutableStateOf("") }
    var employeePassword by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") }
    var menuName by remember { mutableStateOf("") }
    var menuPrice by remember { mutableStateOf("") }
    var menuCategory by remember { mutableStateOf("Кальяны") }
    var role by remember { mutableStateOf(Role.EMPLOYEE) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Админ-панель", style = MaterialTheme.typography.headlineSmall) }
        item { Button(onClick = onLogout) { Text("Выйти") } }

        item { Text("Сотрудники") }
        items(employees) { Text("${it.login} (${it.role})") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = employeeLogin, onValueChange = { employeeLogin = it }, label = { Text("Логин") })
                OutlinedTextField(
                    value = employeePassword,
                    onValueChange = { employeePassword = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { role = Role.EMPLOYEE }) { Text("Сотрудник") }
                Button(onClick = { role = Role.ADMIN }) { Text("Админ") }
            }
        }
        item {
            Button(onClick = {
                if (employeeLogin.isNotBlank() && employeePassword.isNotBlank()) {
                    employees.add(
                        Employee(
                            login = employeeLogin,
                            passwordHash = hashPassword(employeePassword),
                            role = role
                        )
                    )
                    employeeLogin = ""; employeePassword = ""
                }
            }) { Text("Добавить сотрудника") }
        }

        item { Text("Столы") }
        items(tables) { Text("Стол №${it.number}") }
        item { OutlinedTextField(value = tableNumber, onValueChange = { tableNumber = it }, label = { Text("Номер стола") }) }
        item {
            Button(onClick = {
                tableNumber.toIntOrNull()?.let { n -> tables.add(HallTable(number = n)); tableNumber = "" }
            }) { Text("Добавить стол") }
        }

        item { Text("Меню") }
        items(menu) { Text("${it.name} • ${it.category} • ${it.price}") }
        item { OutlinedTextField(value = menuName, onValueChange = { menuName = it }, label = { Text("Название") }) }
        item { OutlinedTextField(value = menuCategory, onValueChange = { menuCategory = it }, label = { Text("Категория") }) }
        item { OutlinedTextField(value = menuPrice, onValueChange = { menuPrice = it }, label = { Text("Цена") }) }
        item {
            Button(onClick = {
                val p = menuPrice.toDoubleOrNull()
                if (menuName.isNotBlank() && p != null) {
                    menu.add(MenuItem(name = menuName, price = p, category = menuCategory))
                    menuName = ""; menuPrice = ""
                }
            }) { Text("Добавить позицию") }
        }

        item { Text("Скидки: только ${ALLOWED_DISCOUNTS.joinToString("%, ")}%") }
    }
}

@Composable
fun EmployeeScreen(
    tables: List<HallTable>,
    menu: List<MenuItem>,
    receipts: MutableList<Receipt>,
    onLogout: () -> Unit
) {
    var selectedTable by remember { mutableStateOf<HallTable?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(5_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Карта столов", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onLogout) { Text("Выйти") }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tables) { table ->
                val tableReceipts = receipts.filter { it.tableId == table.id && !it.closed }
                val warn = tableReceipts.any {
                    it.timerStartedAt?.let { startedAt ->
                        val minutes = Duration.between(startedAt, now).toMinutes()
                        minutes >= 75 && minutes < 90
                    } == true
                }
                Card(
                    modifier = Modifier.size(100.dp).clickable { selectedTable = table }
                        .background(Color.Transparent),
                    colors = CardDefaults.cardColors(
                        containerColor = if (warn) Color.Red.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Стол ${table.number}")
                        Text("Чеков: ${tableReceipts.size}")
                    }
                }
            }
        }

        selectedTable?.let { table ->
            val tableReceipts = receipts.filter { it.tableId == table.id && !it.closed }
            Text("Выбран стол ${table.number}")
            Button(onClick = {
                val receipt = Receipt(tableId = table.id)
                receipt.timerStartedAt = Instant.now()
                receipts.add(receipt)
            }) { Text("Открыть новый чек + таймер") }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tableReceipts) { check ->
                    Card(Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Чек ${check.id.take(6)}")
                            menu.forEach { mi ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${mi.name} (${mi.price})")
                                    Button(onClick = { check.items.add(ReceiptItem(mi, 1)) }) { Text("+") }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ALLOWED_DISCOUNTS.forEach { d ->
                                    Button(onClick = { check.discount = d }) { Text("Скидка $d%") }
                                }
                            }

                            val subtotal = check.items.sumOf { it.menuItem.price * it.qty }
                            val total = subtotal * (1 - check.discount / 100.0)
                            Text("Итого: $total")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { check.cardPaid = total; check.cashPaid = 0.0; check.closed = true }) { Text("Оплата картой") }
                                Button(onClick = { check.cashPaid = total; check.cardPaid = 0.0; check.closed = true }) { Text("Оплата наличными") }
                                Button(onClick = {
                                    check.cardPaid = total / 2
                                    check.cashPaid = total / 2
                                    check.closed = true
                                }) { Text("50/50") }
                            }
                            Button(onClick = { check.timerStartedAt = Instant.now() }) { Text("Запустить новый таймер") }
                        }
                    }
                }
            }
        }
    }
}
