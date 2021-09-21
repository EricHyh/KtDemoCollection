package com.hyh.paging3demo.list.fragment.item

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.FlatListItem
import com.hyh.list.TypedViewHolderFactory
import com.hyh.list.decoration.CardPosition
import com.hyh.page.state.PageStateController
import com.hyh.page.state.UnitState

class UniversalAssetsItem(
    controller: PageStateController
) : AbsPageUnitItem<UniversalAssetsHolder>(controller) {

    override val initialState: UnitState
        get() = UnitState.SUCCESS


    override fun getViewHolderFactory(): TypedViewHolderFactory<UniversalAssetsHolder> {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(viewHolder: UniversalAssetsHolder) {
        TODO("Not yet implemented")
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        TODO("Not yet implemented")
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCardPosition(): CardPosition {
        TODO("Not yet implemented")
    }

    override fun getItemOffsets(outRect: Rect) {
        TODO("Not yet implemented")
    }
}


class UniversalAssetsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


}