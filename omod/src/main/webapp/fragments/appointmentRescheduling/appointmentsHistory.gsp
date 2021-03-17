<%
    ui.decorateWith("kenyaemr", "standardPage", [ patient: currentPatient, layout: "sidebar" ])
    ui.decorateWith("kenyaui", "panel", [ heading: "Hiv Greencard History" ])

    def onEncounterClick = { encounter ->
        """kenyaemr.openEncounterDialog('${currentApp.id}', ${encounter.id});"""
    }

%>
<style>
.simple-table {
    border: solid 1px #DDEEEE;
    border-collapse: collapse;
    border-spacing: 0;
    font: normal 13px Arial, sans-serif;
}
.simple-table thead th {
    background-color: #DDEFEF;
    border: solid 1px #DDEEEE;
    color: #336B6B;
    padding: 10px;
    text-align: left;
    text-shadow: 1px 1px 1px #fff;
}
.simple-table td {
    border: solid 1px #DDEEEE;
    color: #333;
    padding: 5px;
    text-shadow: 1px 1px 1px #fff;
}
</style>


<div>

    <fieldset>
        <legend>HIV GreenCard History</legend>
        <%if (encounters) { %>
        <table class="simple-table">
            <tr>
                <th align="left" width="15%">Last Encounter Date</th>
                <th align="left" width="15%">Next Appointment Date</th>
                <th align="left" width="15%">Update Next Appointment Date</th>
             </tr>
            <% encounters.each { %>
            <tr>
                <td>${it.encDate}</td>
                <td>${it.tcaDate} </td>
                <td> <span id="update-tca-button-placeholder"></span></td>

            </tr>
            <% } %>
        </table>
        <% } else {%>
        <div>No history found</div>

        <% } %>
    </fieldset>

</div>

<script type="text/javascript">
    //On ready
    jQuery(function () {

        jQuery('#update-tca-button').appendTo(jQuery('#update-tca-button-placeholder'));

    }); // end of jQuery initialization block

 </script>

<!-- You can't nest forms in HTML, so keep the dialog box form down here -->
${ui.includeFragment("kenyaui", "widget/dialogForm", [
        buttonConfig     : [id: "update-tca-button", label: "Update", iconProvider: "kenyaui", icon: "glyphs/calculate.png"],
        dialogConfig     : [heading: "Update Next Appointment Date", width: 40, height: 40],
        fields           : [
                      [ hiddenInputName: "patientId", value: currentPatient.id ],
                      [
                        label: "Next Appointment Date", formFieldName: "tcaDate",
                        class: java.util.Date, initialValue: new java.text.SimpleDateFormat("yyyy-MM-dd").parse((new Date().getYear() + 1900) + "-06-15")
                      ]
        ],
        fragmentProvider : "knh",
        fragment         : "knhUtils",
        action           : "updateTCADate",
        onSuccessCallback: "ui.reloadPage()",
        onOpenCallback   : """jQuery('input[name="tcaDate"]').focus()""",
        submitLabel      : ui.message("general.submit"),
        cancelLabel      : ui.message("general.cancel")
])}
