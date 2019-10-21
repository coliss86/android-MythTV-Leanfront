/*
 * Copyright (c) 2016 The Android Open Source Project
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

package org.mythtv.leanfront.data;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * VideoContract represents the contract for storing videos in the SQLite database.
 */
public final class VideoContract {
    // The name for the entire content provider.
    public static final String CONTENT_AUTHORITY = "org.mythtv.leanfront";
    // Base of all URIs that will be used to contact the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    // The content paths.
    public static final String PATH_VIDEO = "video";

    public static final class VideoEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_VIDEO).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "." + PATH_VIDEO;
        // Name of the video table.
        public static final String TABLE_NAME = "video";
        // Column for category = title
        public static final String COLUMN_CATEGORY = SearchManager.SUGGEST_COLUMN_TEXT_1;;
        public static final String COLUMN_TITLE = SearchManager.SUGGEST_COLUMN_TEXT_1;;
        // Description of the video.
        public static final String COLUMN_DESC = "description";
        // Description of the video.
        public static final String COLUMN_SUBTITLE = SearchManager.SUGGEST_COLUMN_TEXT_2;
        // The url to the video content.
        public static final String COLUMN_VIDEO_URL = "video_url";
        // The url to the background image.
        public static final String COLUMN_BG_IMAGE_URL = "bg_image_url";
        // The studio name.
        public static final String COLUMN_STUDIO = "studio";
        // The card image for the video.
        public static final String COLUMN_CARD_IMG = SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE;
        // The content type of the video.
        public static final String COLUMN_CONTENT_TYPE = SearchManager.SUGGEST_COLUMN_CONTENT_TYPE;
        // The year the video was produced.
        public static final String COLUMN_PRODUCTION_YEAR = SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR;
        // The duration of the video.
        public static final String COLUMN_DURATION = SearchManager.SUGGEST_COLUMN_DURATION;
        // The original air date string yyyy-mm-dd
        public static final String COLUMN_AIRDATE  = "airdate";
        // The start time string format 2018-08-13T20:30:00Z
        public static final String COLUMN_STARTTIME  = "starttime";
        // MythTV values
        public static final String COLUMN_RECORDEDID = "recordedid";
        public static final String COLUMN_STORAGEGROUP = "storagegroup";
        // Empty Recgroup indicates a video file instead of a recording.
        public static final String COLUMN_RECGROUP = "recgroup";
        public static final String COLUMN_SEASON = "season";
        public static final String COLUMN_EPISODE = "episode";
        // The action intent for the result.
        public static final String COLUMN_ACTION = SearchManager.SUGGEST_COLUMN_INTENT_ACTION;
        // Returns the Uri referencing a video with the specified id.
        public static Uri buildVideoUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    } // end of VideoEntry

        /* Inner class that defines the status table */
    public static class StatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "videostatus";
        public static final String COLUMN_VIDEO_URL = "video_url";
        public static final String COLUMN_LAST_USED = "last_used";
        public static final String COLUMN_BOOKMARK = "bookmark";
    }

}
