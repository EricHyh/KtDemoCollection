package com.hyh.list

abstract class MultiTabsItemSource<Param : Any> : ItemsBucketSource<Param>() {

    companion object {
        private const val TITLE_BUCKET_ID = 0
        private const val CONTENT_BUCKET_ID = 1
    }

    init {
        registerItemsBucketIds(listOf(TITLE_BUCKET_ID, CONTENT_BUCKET_ID))
    }

    override suspend fun getPreShow(
        param: Param,
        displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?
    ): BucketPreShowResult {
        val tabToken = getTabTokenFromParam(param)
        val contentBucket = displayedItemsBucketMap?.get(CONTENT_BUCKET_ID)
        val displayedContentItemsToken = contentBucket?.itemsToken
        if (tabToken == displayedContentItemsToken) {
            return if (isEmptyContent(contentBucket.items)) {
                val titleItems = getTitlePreShow(tabToken, param)
                val contentItems = getContentPreShow(tabToken, param)
                val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
                itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
                itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, tabToken, contentItems)
                BucketPreShowResult.Success(itemsBucketIds, itemsBucketMap)
            } else {
                BucketPreShowResult.Unused
            }
        }
        val titleItems = getTitlePreShow(tabToken, param)
        val contentItemsBucket = storage.get(CONTENT_BUCKET_ID, tabToken)
        val items = contentItemsBucket?.items

        val contentItems = if (items == null || isEmptyContent(items)) {
            getContentPreShow(tabToken, param)
        } else {
            items
        }
        val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
        itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
        itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, tabToken, contentItems)
        return BucketPreShowResult.Success(itemsBucketIds, itemsBucketMap)
    }

    override suspend fun load(
        param: Param,
        displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?
    ): BucketLoadResult {
        val tabToken = getTabTokenFromParam(param)
        val titleItems = getTitlePreShow(tabToken, param)
        return when (val contentResult = getContent(tabToken, param)) {
            is ContentResult.Error -> {
                BucketLoadResult.Error(contentResult.error)
            }
            is ContentResult.Success -> {
                val contentItems = contentResult.items
                val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
                itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
                itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, tabToken, contentItems)
                BucketLoadResult.Success(itemsBucketIds, itemsBucketMap)
            }
        }
    }

    protected abstract fun isEmptyContent(items: List<ItemData>): Boolean
    protected abstract suspend fun getTitlePreShow(tabToken: Any, param: Param): List<ItemData>
    protected abstract suspend fun getContentPreShow(tabToken: Any, param: Param): List<ItemData>
    protected abstract suspend fun getContent(tabToken: Any, param: Param): ContentResult
    protected abstract fun getTabTokenFromParam(param: Param): Any

    sealed class ContentResult {

        class Error(
            val error: Throwable
        ) : ContentResult()

        class Success(val items: List<ItemData>) : ContentResult()
    }
}