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

package com.aurora.store.viewmodel.review

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.gplayapi.helpers.ReviewsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewsHelper: ReviewsHelper
) : ViewModel() {
    val TAG = javaClass.simpleName

    val liveData: MutableLiveData<ReviewCluster> = MutableLiveData()

    private lateinit var reviewsCluster: ReviewCluster

    fun fetchReview(packageName: String, filter: Review.Filter) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reviewsCluster = reviewsHelper.getReviews(packageName, filter)
                liveData.postValue(reviewsCluster)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch reviews", e)
            }
        }
    }

    fun next(nextReviewPageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentCluster = reviewsCluster
                val nextReviewCluster = reviewsHelper.next(nextReviewPageUrl)

                reviewsCluster = currentCluster.copy(
                    nextPageUrl = nextReviewCluster.nextPageUrl,
                    reviewList = currentCluster.reviewList + nextReviewCluster.reviewList
                )

                liveData.postValue(reviewsCluster)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch next reviews $nextReviewPageUrl", e)
            }
        }
    }
}
