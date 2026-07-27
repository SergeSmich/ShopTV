package com.example.shoptv.network

import com.example.shoptv.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mock repository с тестовыми товарами для разработки UI
 */
class MockProductRepository {

    suspend fun getPopularProducts(): List<Product> = withContext(Dispatchers.Default) {
        listOf(
            Product(
                id = "1",
                title = "Молоко Лента цельное 2.5%",
                price = 89.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Milk"
            ),
            Product(
                id = "2",
                title = "Хлеб Пшеничный порционный",
                price = 45.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Bread"
            ),
            Product(
                id = "3",
                title = "Яйца куриные C0 10шт",
                price = 129.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Eggs"
            ),
            Product(
                id = "4",
                title = "Масло сливочное Лента 180г",
                price = 299.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Butter"
            ),
            Product(
                id = "5",
                title = "Сыр плавленый Лента 200г",
                price = 169.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Cheese"
            ),
            Product(
                id = "6",
                title = "Йогурт питьевой 2.5% 200мл",
                price = 59.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Yogurt"
            ),
            Product(
                id = "7",
                title = "Творог Лента 5% 200г",
                price = 79.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Cottage"
            ),
            Product(
                id = "8",
                title = "Булка с маком Лента 1шт",
                price = 35.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Bun"
            ),
            Product(
                id = "9",
                title = "Печенье овсяное Лента 200г",
                price = 99.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Cookies"
            ),
            Product(
                id = "10",
                title = "Сок апельсиновый Лента 1л",
                price = 119.0,
                imageUrl = "https://via.placeholder.com/200x150?text=Juice"
            )
        )
    }

    suspend fun searchProducts(query: String): List<Product> = withContext(Dispatchers.Default) {
        getPopularProducts().filter { 
            it.title.contains(query, ignoreCase = true) 
        }
    }
}
