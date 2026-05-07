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

package com.aurora.store.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.store.databinding.ViewHeaderUpdateBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class UpdateHeaderView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewHeaderUpdateBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun title(title: String) {
        binding.txtTitle.text = title
    }

    @ModelProp(ModelProp.Option.NullOnRecycle)
    fun subtitle(subtitle: String?) {
        if (subtitle.isNullOrBlank()) {
            binding.txtSubtitle.visibility = View.GONE
            binding.txtSubtitle.text = null
        } else {
            binding.txtSubtitle.visibility = View.VISIBLE
            binding.txtSubtitle.text = subtitle
        }
    }

    @ModelProp(ModelProp.Option.NullOnRecycle)
    fun action(action: String?) {
        if (action.isNullOrBlank()) {
            binding.btnAction.visibility = View.GONE
            binding.btnAction.text = null
        } else {
            binding.btnAction.visibility = View.VISIBLE
            binding.btnAction.text = action
        }
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        binding.btnAction.setOnClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {
        binding.btnAction.isEnabled = true
    }
}
