package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.repository.WidgetConfigRepository
import javax.inject.Inject

class GetAllWidgetConfigsUseCase @Inject constructor(
    private val widgetConfigRepository: WidgetConfigRepository
) {
    suspend operator fun invoke(): List<WidgetConfigEntity> =
        widgetConfigRepository.getAll()
}
