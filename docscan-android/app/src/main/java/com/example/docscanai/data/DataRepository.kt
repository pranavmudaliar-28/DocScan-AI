package com.example.docscanai.data

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    val data: Flow<List<ScanRecord>>
}

class DefaultDataRepository : DataRepository {
    override val data: Flow<List<ScanRecord>> = ScanHistoryRepository.scans
}
