/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.viewmodel.category

import android.app.Application
import com.aurora.gplayapi.data.models.Category
import com.aurora.store.data.RequestState

class AppCategoryViewModel(application: Application) : BaseCategoryViewModel(application) {

    init {
        type = Category.Type.APPLICATION
        requestState = RequestState.Init
        observe()
    }
}

class GameCategoryViewModel(application: Application) : BaseCategoryViewModel(application) {

    init {
        type = Category.Type.GAME
        requestState = RequestState.Init
        observe()
    }
}