package com.cafarovceyxun.anamuslim.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity

data class AyahWithWords(
    @Embedded
    val ayah: AyahEntity,

    @Relation(
        parentColumn = "ayah_id",
        entityColumn = "ayah_id"
    )
    val words: List<AyahWordEntity>
)
