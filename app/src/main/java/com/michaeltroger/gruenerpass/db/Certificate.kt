package com.michaeltroger.gruenerpass.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "certificates")
data class Certificate (
    /**
     * The name of the internally stored pdf file
     */
    @PrimaryKey val id: String,
    /**
     * The user defined und user facing document name
     */
    @ColumnInfo(name = "name") val name: String,
) : Parcelable
