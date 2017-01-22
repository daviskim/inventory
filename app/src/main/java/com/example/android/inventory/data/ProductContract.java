package com.example.android.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by DK on 8/27/2016.
 */
public final class ProductContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ProductContract() {}

    /**
     * The "Content authority" is a name for the entire content provider.
     * A convenient string to use for the content authority is the package name for the app.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.inventory";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's)
     */
    public static final String PATH_PRODUCTS = "inventory";

    /**
     * Inner class that defines constant values for the inventory database table.
     * Each entry in the table represents a single product.
     */
    public static final class ProductEntry implements BaseColumns {

        // The content URI to access the product data in the provider.
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products.
         */
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
                "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
                "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        // Name of database table for inventory
        public final static String TABLE_NAME = "inventory";

        // Unique ID number for the product (only for use in the database table).
        // Type: INTEGER
        public final static String _ID = BaseColumns._ID;

        // Product name.
        // Type: TEXT
        public final static String COLUMN_PRODUCT_NAME = "name";

        // Product price.
        // Type: Double
        public final static String COLUMN_PRODUCT_PRICE = "price";

        // Product quantity.
        // Type: Integer
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";

        // Product quantity sold.
        // Type: Integer
        public final static String COLUMN_PRODUCT_SOLD = "sold";

        // Product image.
        // Type: byte[]
        public final static String COLUMN_PRODUCT_IMAGE = "image";
    }
}
