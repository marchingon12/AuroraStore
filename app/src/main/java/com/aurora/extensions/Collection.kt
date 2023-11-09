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

package com.aurora.extensions

fun <T> MutableList<T>.flushAndAdd(list: List<T>) {
    clear()
    addAll(list)
}

fun <T> MutableSet<T>.flushAndAdd(list: Set<T>) {
    clear()
    addAll(list)
}

fun <T> MutableSet<T>.copyAndAdd(element: T): MutableSet<T> {
    val newSet = this.toMutableSet()
    newSet.add(element)
    return newSet
}

fun <T> MutableSet<T>.copyAndRemove(element: T): MutableSet<T> {
    val newSet = this.toMutableSet()
    newSet.remove(element)
    return newSet
}
