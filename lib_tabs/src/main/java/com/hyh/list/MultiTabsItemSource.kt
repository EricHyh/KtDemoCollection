package com.hyh.list

abstract class MultiTabsItemSource<Param : Any> : ItemSource<Param>() {

    companion object {
        private const val TITLE_BUCKET_ID = 0
        private const val CONTENT_BUCKET_ID = 1
    }

    init {
        registerItemsBucketIds(listOf(TITLE_BUCKET_ID, CONTENT_BUCKET_ID))
    }

    final override suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult {
        val tabToken = getTabTokenFromParam(params.param)
        val displayedContentItemsToken = params.displayedItemsBucketMap?.get(CONTENT_BUCKET_ID)?.itemsToken
        if (tabToken == displayedContentItemsToken) {
            return PreShowResult.Unused
        }
        val titleItems = getTitlePreShow(tabToken, params.param)
        val contentItemsBucket = delegate.storage.get(CONTENT_BUCKET_ID, tabToken)
        val contentItems = contentItemsBucket?.items ?: getContentPreShow(tabToken, params.param)

        val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
        itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
        itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, tabToken, contentItems)
        return PreShowResult.Success(itemsBucketIds, itemsBucketMap)

    }

    final override suspend fun load(params: LoadParams<Param>): LoadResult {
        val tabToken = getTabTokenFromParam(params.param)
        val titleItems = getTitlePreShow(tabToken, params.param)
        return when (val contentResult = getContent(tabToken, params.param)) {
            is ContentResult.Error -> {
                LoadResult.Error(contentResult.error)
            }
            is ContentResult.Success -> {
                val contentItems = contentResult.items
                val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
                itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
                itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, tabToken, contentItems)
                LoadResult.Success(itemsBucketIds, itemsBucketMap)
            }
        }
    }

    protected abstract suspend fun getTitlePreShow(tabToken: Any, param: Param): List<ItemData>
    protected abstract suspend fun getContentPreShow(tabToken: Any, param: Param): List<ItemData>
    protected abstract suspend fun getContent(tabToken: Any, param: Param): ContentResult

    protected abstract fun getTabTokenFromParam(param: Param): Any

    final override fun shouldCacheBucket(itemsBucket: ItemsBucket): Boolean = true

    sealed class ContentResult {

        class Error(
            val error: Throwable
        ) : ContentResult()

        class Success(val items: List<ItemData>) : ContentResult()
    }
}