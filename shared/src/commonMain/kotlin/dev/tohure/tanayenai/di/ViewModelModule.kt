package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        factory { (userId: String) ->
            PantryViewModel(
                pantryRepository = get(),
                userId = userId,
            )
        }
    }
