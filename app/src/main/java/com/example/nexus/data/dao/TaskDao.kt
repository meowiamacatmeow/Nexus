package com.example.nexus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.nexus.data.entity.TaskEntity
import com.example.nexus.data.entity.TaskStepEntity
import com.example.nexus.data.entity.TaskWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasksWithSteps(): Flow<List<TaskWithSteps>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithSteps(taskId: String): Flow<TaskWithSteps?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskSteps(steps: List<TaskStepEntity>)

    @Update
    suspend fun updateTaskStep(step: TaskStepEntity)

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: String)
}
