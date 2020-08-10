package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.text.method.TextKeyListener
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import java.util.regex.Pattern
import kotlin.properties.Delegates

class PostalCodeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    internal var config: Config by Delegates.observable(
        Config.Global
    ) { _, oldValue, newValue ->
        when (newValue) {
            Config.US -> configureForUs()
            else -> configureForGlobal()
        }
    }

    /**
     * we need to support US card in any locale
     */
    internal val postalCode: String?
        get() {
            return when (config) {
                Config.US -> {
                    fieldText.takeIf {
                        ZIP_CODE_PATTERN.matcher(fieldText).matches()
                    }
                }
                Config.CANADA -> {
                    fieldText.takeIf {
                        it.length in MAX_LENGTH_US..MAX_LENGTH_CANADA
                    }
                }
                Config.AUSTRALIA -> {
                    fieldText.takeIf {
                        it.length in MAX_LENGTH_AUSTRALIA..MAX_LENGTH_US
                    }
                }
                else -> {
                    fieldText
                }
            }
        }

    init {
        setErrorMessage(resources.getString(R.string.invalid_zip))
        maxLines = 1

        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                shouldShowError = false
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        configureForGlobal()
    }

    /**
     * Configure the field for United States users
     */
    private fun configureForUs() {
        updateHint(R.string.address_label_zip_code)
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_US))
        keyListener = DigitsKeyListener.getInstance(false, true)
        inputType = InputType.TYPE_CLASS_NUMBER
    }

    /**
     * Configure the field for global users
     */
    private fun configureForGlobal() {
        updateHint(R.string.address_label_postal_code)
        when(config) {
            Config.Global -> {
                filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_GLOBAL))
                keyListener = TextKeyListener.getInstance()
                inputType = InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            }
            Config.CANADA -> {
                filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_CANADA))
                keyListener = TextKeyListener.getInstance()
                inputType = InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            }
            Config.AUSTRALIA -> {
                filters = arrayOf(InputFilter.LengthFilter(kotlin.math.max(MAX_LENGTH_AUSTRALIA, MAX_LENGTH_US)))
                keyListener = DigitsKeyListener.getInstance(false, true)
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            else -> return
        }
    }

    /**
     * If a `TextInputLayout` is an ancestor of this view, set the hint on it. Otherwise, set
     * the hint on this view.
     */
    private fun updateHint(@StringRes hintRes: Int) {
        getTextInputLayout()?.let {
            if (it.isHintEnabled) {
                it.hint = resources.getString(hintRes)
            } else {
                setHint(hintRes)
            }
        }
    }

    /**
     * Copied from `TextInputEditText`
     */
    private fun getTextInputLayout(): TextInputLayout? {
        var parent = parent
        while (parent is View) {
            if (parent is TextInputLayout) {
                return parent
            }
            parent = parent.getParent()
        }
        return null
    }

    internal enum class Config {
        Global,
        US,
        CANADA,
        AUSTRALIA
    }

    private companion object {
        private const val MAX_LENGTH_US = 5
        private const val MAX_LENGTH_CANADA = 7
        private const val MAX_LENGTH_AUSTRALIA = 4
        private const val MAX_LENGTH_GLOBAL = 13

        private val ZIP_CODE_PATTERN = Pattern.compile("^[0-9]{5}$")
    }
}
