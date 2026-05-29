package com.example.docscanai.data.local

import android.content.Context

object DatabaseModule {
    private var _database: AppDatabase? = null

    val database: AppDatabase
        get() = _database ?: error("DatabaseModule not initialised — call DatabaseModule.init(context) first")

    fun init(context: Context) {
        if (_database != null) return
        _database = AppDatabase.getDatabase(context)
    }

    val documentDao: DocumentDao
        get() = database.documentDao()

    val folderDao: FolderDao
        get() = database.folderDao()

    val tagDao: TagDao
        get() = database.tagDao()

    val categoryDao: CategoryDao
        get() = database.categoryDao()
        
    val signatureDao: SignatureDao
        get() = database.signatureDao()
        
    val annotationDao: AnnotationDao
        get() = database.annotationDao()
        
    val localDocumentRepository: LocalDocumentRepository by lazy {
        LocalDocumentRepository(documentDao, folderDao, tagDao, categoryDao)
    }

    val signatureRepository: SignatureRepository by lazy {
        SignatureRepository(signatureDao)
    }
}
