package com.example.docscanai.data.local

import kotlinx.coroutines.flow.Flow

class SignatureRepository(
    private val signatureDao: SignatureDao
) {
    val allSignatures: Flow<List<SignatureEntity>> = signatureDao.getAllSignatures()

    suspend fun getSignatureById(id: String): SignatureEntity? {
        return signatureDao.getSignatureById(id)
    }

    suspend fun insertSignature(signature: SignatureEntity) {
        signatureDao.insertSignature(signature)
    }

    suspend fun updateSignature(signature: SignatureEntity) {
        signatureDao.updateSignature(signature)
    }

    suspend fun deleteSignature(signature: SignatureEntity) {
        signatureDao.deleteSignature(signature)
    }
}
