/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * This class provides global access to the centralized AppSearch index maintained by the system.
 *
 * <p>Apps can retrieve indexed documents through the query API.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/148046169): This class header needs a detailed example/tutorial.
public class GlobalAppSearchManager {

    private final AppSearchBackend mBackend;

    /** Builder class for {@link GlobalAppSearchManager} objects. */
    public static final class Builder {
        private AppSearchBackend mBackend;
        private boolean mBuilt = false;

        /**
         * Sets the backend where this {@link GlobalAppSearchManager} will retrieve data from.
         */
        @NonNull
        public Builder setBackend(@NonNull AppSearchBackend backend) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(backend);
            mBackend = backend;
            return this;
        }

        /**
         * Asynchronously connects to the AppSearch backend and returns the initialized instance.
         */
        @NonNull
        public ListenableFuture<AppSearchResult<GlobalAppSearchManager>> build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkState(mBackend != null, "setBackend() has never been called");
            mBuilt = true;
            ResolvableFuture<AppSearchResult<GlobalAppSearchManager>> result =
                    ResolvableFuture.create();
            result.set(AppSearchResult.newSuccessfulResult(new GlobalAppSearchManager(mBackend)));
            return result;
        }
    }

    GlobalAppSearchManager(@NonNull AppSearchBackend backend) {
        mBackend = backend;
    }

    /**
     * Searches across all documents in the backend based on a given query string.
     *
     * <p>Currently we support following features in the raw query format:
     * <ul>
     *     <li>AND
     *     <p>AND joins (e.g. “match documents that have both the terms ‘dog’ and
     *     ‘cat’”).
     *     Example: hello world matches documents that have both ‘hello’ and ‘world’
     *     <li>OR
     *     <p>OR joins (e.g. “match documents that have either the term ‘dog’ or
     *     ‘cat’”).
     *     Example: dog OR puppy
     *     <li>Exclusion
     *     <p>Exclude a term (e.g. “match documents that do
     *     not have the term ‘dog’”).
     *     Example: -dog excludes the term ‘dog’
     *     <li>Grouping terms
     *     <p>Allow for conceptual grouping of subqueries to enable hierarchical structures (e.g.
     *     “match documents that have either ‘dog’ or ‘puppy’, and either ‘cat’ or ‘kitten’”).
     *     Example: (dog puppy) (cat kitten) two one group containing two terms.
     *     <li>Property restricts
     *     <p> Specifies which properties of a document to specifically match terms in (e.g.
     *     “match documents where the ‘subject’ property contains ‘important’”).
     *     Example: subject:important matches documents with the term ‘important’ in the
     *     ‘subject’ property
     *     <li>Schema type restricts
     *     <p>This is similar to property restricts, but allows for restricts on top-level document
     *     fields, such as schema_type. Clients should be able to limit their query to documents of
     *     a certain schema_type (e.g. “match documents that are of the ‘Email’ schema_type”).
     *     Example: { schema_type_filters: “Email”, “Video”,query: “dog” } will match documents
     *     that contain the query term ‘dog’ and are of either the ‘Email’ schema type or the
     *     ‘Video’ schema type.
     * </ul>
     *
     * <p> This method is lightweight. The heavy work will be done in
     * {@link SearchResults#getNextPage()}.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @return The search result of performing this operation.
     */
    @NonNull
    public SearchResults globalQuery(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        AppSearchBackend.BackendSearchResults backendSearchResults =
                mBackend.globalQuery(queryExpression, searchSpec);
        return new SearchResults(backendSearchResults);
    }
}
