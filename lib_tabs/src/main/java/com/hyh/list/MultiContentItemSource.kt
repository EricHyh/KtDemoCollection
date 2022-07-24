package com.hyh.list

abstract class MultiContentItemSource<Param> : ItemsBucketSource<Param>() {

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
        val contentToken = getContentTokenFromParam(param)
        val contentBucket = displayedItemsBucketMap?.get(CONTENT_BUCKET_ID)
        val displayedContentItemsToken = contentBucket?.itemsToken
        if (contentToken == displayedContentItemsToken) {
            return if (isEmptyContent(contentBucket.items)) {
                val titleItems = getTitlePreShow(contentToken, param)
                val contentItems = getContentPreShow(contentToken, param)
                val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
                itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
                itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, contentToken, contentItems)
                BucketPreShowResult.Success(itemsBucketIds, itemsBucketMap)
            } else {
                BucketPreShowResult.Unused
            }
        }
        val titleItems = getTitlePreShow(contentToken, param)
        val contentItemsBucket = storage.get(CONTENT_BUCKET_ID, contentToken)
        val items = contentItemsBucket?.items

        val contentItems = if (items == null || isEmptyContent(items)) {
            getContentPreShow(contentToken, param)
        } else {
            items
        }
        val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
        itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
        itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, contentToken, contentItems)
        return BucketPreShowResult.Success(itemsBucketIds, itemsBucketMap)
    }

    override suspend fun load(
        param: Param,
        displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?
    ): BucketLoadResult {
        val contentToken = getContentTokenFromParam(param)
        val titleItems = getTitlePreShow(contentToken, param)
        return when (val contentResult = getContent(contentToken, param)) {
            is ContentResult.Error -> {
                BucketLoadResult.Error(contentResult.error)
            }
            is ContentResult.Success -> {
                val contentItems = contentResult.items
                val itemsBucketMap: MutableMap<Int, ItemsBucket> = mutableMapOf()
                itemsBucketMap[TITLE_BUCKET_ID] = ItemsBucket(TITLE_BUCKET_ID, DEFAULT_ITEMS_TOKEN, titleItems)
                itemsBucketMap[CONTENT_BUCKET_ID] = ItemsBucket(CONTENT_BUCKET_ID, contentToken, contentItems)
                BucketLoadResult.Success(itemsBucketIds, itemsBucketMap)
            }
        }
    }

    protected abstract fun isEmptyContent(items: List<FlatListItem>): Boolean
    protected abstract suspend fun getTitlePreShow(contentToken: Any, param: Param): List<FlatListItem>
    protected abstract suspend fun getContentPreShow(contentToken: Any, param: Param): List<FlatListItem>
    protected abstract suspend fun getContent(contentToken: Any, param: Param): ContentResult
    protected abstract fun getContentTokenFromParam(param: Param): Any

    sealed class ContentResult {

        class Error(
            val error: Throwable
        ) : ContentResult()

        class Success(val items: List<FlatListItem>) : ContentResult()
    }
}