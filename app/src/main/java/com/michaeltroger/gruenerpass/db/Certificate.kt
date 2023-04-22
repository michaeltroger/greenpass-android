package com.michaeltroger.gruenerpass.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class Certificate (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
)
