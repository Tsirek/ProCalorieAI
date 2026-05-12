package com.procalorieai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealHistoryAdapter
    private lateinit var tvWeeklyAvg: TextView
    private lateinit var tvWeekRange: TextView
    private lateinit var tvEmpty: TextView

    private var mealsList = mutableListOf<GetMealLogsQuery.Meal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_history_activity)

        TokenManager.init(this)

        tvWeeklyAvg = findViewById(R.id.tvWeeklyAvg)
        tvWeekRange = findViewById(R.id.tvWeekRange)
        tvEmpty = findViewById(R.id.tvEmpty)
        recyclerView = findViewById(R.id.rvHistory)

        adapter = MealHistoryAdapter(
            onDelete = { mealId ->
                deleteMeal(mealId)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fmt = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = fmt.format(cal.time)
        tvWeekRange.text = "$weekStart – $weekEnd"

        loadMeals()
    }

    private fun loadMeals() {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().query(GetMealLogsQuery())
                    .execute()

                response.data?.meals?.let { meals ->
                    mealsList = meals.toMutableList()
                    adapter.submitList(mealsList)
                    tvEmpty.visibility = if (mealsList.isEmpty()) View.VISIBLE else View.GONE
                    calculateWeeklyAvg()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@HistoryActivity, "Ошибка загрузки истории", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun calculateWeeklyAvg() {
        if (mealsList.isEmpty()) {
            tvWeeklyAvg.text = "—"
            return
        }

        // Группируем по дате и считаем сумму за каждый день
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val caloriesByDay = mealsList.groupBy { meal ->
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(meal.eatenAt.toString())
                sdf.format(parsed ?: Date())
            } catch (e: Exception) { "unknown" }
        }.mapValues { (_, meals) -> meals.sumOf { it.nutrition.calories } }

        val avg = if (caloriesByDay.isNotEmpty())
            caloriesByDay.values.sum() / caloriesByDay.size
        else 0.0

        tvWeeklyAvg.text = avg.toInt().toString()
    }

    private fun deleteMeal(mealId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().mutation(DeleteMealMutation(id = mealId))
                    .execute()

                if (response.data?.deleteMealLog == true) {
                    mealsList.removeAll { it.id == mealId }
                    adapter.submitList(mealsList)
                    tvEmpty.visibility = if (mealsList.isEmpty()) View.VISIBLE else View.GONE
                    calculateWeeklyAvg()
                    Toast.makeText(this@HistoryActivity, "Запись удалена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@HistoryActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class MealHistoryAdapter(
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<MealHistoryAdapter.VH>() {

        private var items: List<GetMealLogsQuery.Meal> = emptyList()

        fun submitList(list: List<GetMealLogsQuery.Meal>) {
            items = list
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvMealType: TextView = view.findViewById(R.id.tvMealType)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvCalories: TextView = view.findViewById(R.id.tvCalories)
            val tvMealName: TextView = view.findViewById(R.id.tvMealName)
            val tvProtein: TextView = view.findViewById(R.id.tvProtein)
            val tvCarbs: TextView = view.findViewById(R.id.tvCarbs)
            val tvFat: TextView = view.findViewById(R.id.tvFat)
            val btnDelete: View = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_meal, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val meal = items[position]
            val fmt = SimpleDateFormat("d MMMM, HH:mm", Locale("ru"))
            holder.tvMealType.text = meal.mealType
            holder.tvTime.text = try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                fmt.format(sdf.parse(meal.eatenAt.toString()) ?: java.util.Date())
            } catch (e: Exception) { meal.eatenAt.toString() }
            holder.tvCalories.text = meal.nutrition.calories.toString()
            holder.tvMealName.text = meal.name
            holder.tvProtein.text = "${meal.nutrition.protein.toInt()}g"
            holder.tvCarbs.text = "${meal.nutrition.carbs.toInt()}g"
            holder.tvFat.text = "${meal.nutrition.fat.toInt()}g"
            holder.btnDelete.setOnClickListener { onDelete(meal.id) }
        }
    }
}