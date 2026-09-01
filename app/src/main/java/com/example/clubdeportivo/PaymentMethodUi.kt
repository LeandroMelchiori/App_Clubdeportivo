package com.example.clubdeportivo

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup

object PaymentMethodUi {
    private val optionIds = listOf(
        R.id.rbEfectivo to ManualPaymentMethod.CASH,
        R.id.rbTransferencia to ManualPaymentMethod.TRANSFER,
        R.id.rbTarjeta to ManualPaymentMethod.CARD
    )

    fun bind(group: RadioGroup, configuration: ClubConfiguration, enabled: Boolean = true) {
        group.clearCheck()
        val available = configuration.enabledPaymentMethods()
        optionIds.forEach { (id, method) ->
            group.findViewById<RadioButton>(id).apply {
                text = method.displayName
                visibility = if (method in available) View.VISIBLE else View.GONE
                isEnabled = enabled && method in available
            }
        }
        if (available.size == 1) {
            val selectedId = optionIds.first { it.second == available.single() }.first
            group.check(selectedId)
        }
    }

    fun setEnabled(group: RadioGroup, configuration: ClubConfiguration, enabled: Boolean) {
        val available = configuration.enabledPaymentMethods()
        optionIds.forEach { (id, method) ->
            group.findViewById<RadioButton>(id).isEnabled = enabled && method in available
        }
    }

    fun selected(group: RadioGroup): ManualPaymentMethod? {
        val selectedId = group.checkedRadioButtonId
        return optionIds.firstOrNull { it.first == selectedId }?.second
    }
}
