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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.mythtv.leanfront.data.VideoContract.VideoEntry;
import org.mythtv.leanfront.data.VideoContract.StatusEntry;


/**
 * VideoDbHelper manages the creation and upgrade of the database used in this sample.
 */
public class VideoDbHelper extends SQLiteOpenHelper {

    // Change this when you change the database schema.
    private static final int DATABASE_VERSION = 1;

    // The name of our database.
    private static final String DATABASE_NAME = "leanback.db";

    public VideoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db,0,DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 0) {
            db.execSQL("DROP TABLE IF EXISTS " + VideoEntry.TABLE_NAME);
            // Create a table to hold videos.
            // This table gets deleted and recreated periodically
            final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE " + VideoEntry.TABLE_NAME + " (" +
                    VideoEntry._ID + " INTEGER PRIMARY KEY," +
                    VideoEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_SUBTITLE + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_VIDEO_URL + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_DESC + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_BG_IMAGE_URL + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_STUDIO + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_CARD_IMG + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_CONTENT_TYPE + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_PRODUCTION_YEAR + " TEXT, " +
                    VideoEntry.COLUMN_DURATION + " TEXT NOT NULL, " +
                    VideoEntry.COLUMN_ACTION + " TEXT NOT NULL," +
                    VideoEntry.COLUMN_AIRDATE + " TEXT," +
                    VideoEntry.COLUMN_STARTTIME + " TEXT," +
                    VideoEntry.COLUMN_RECORDEDID + " TEXT," +
                    VideoEntry.COLUMN_STORAGEGROUP + " TEXT," +
                    VideoEntry.COLUMN_RECGROUP + " TEXT," +
                    VideoEntry.COLUMN_SEASON + " TEXT," +
                    VideoEntry.COLUMN_EPISODE + " TEXT" +
                    " );";

            // Do the creating of the table.
            db.execSQL(SQL_CREATE_VIDEO_TABLE);

            db.execSQL("DROP TABLE IF EXISTS " + StatusEntry.TABLE_NAME);
            // videostatus table keeps bookmarks even when video table is reloaded.
            // LAST_USED column is datetime, used to delete entries older than a month.
            final String SQL_CREATE_VIDEOSTATUS_TABLE = "CREATE TABLE " + StatusEntry.TABLE_NAME + " (" +
                    StatusEntry._ID + " INTEGER PRIMARY KEY," +
                    StatusEntry.COLUMN_VIDEO_URL + " TEXT NOT NULL UNIQUE, " +
                    StatusEntry.COLUMN_LAST_USED + " INTEGER NOT NULL, " +
                    StatusEntry.COLUMN_BOOKMARK + " INTEGER);";
            db.execSQL(SQL_CREATE_VIDEOSTATUS_TABLE);
            oldVersion = 1;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}
