<% programs.each { descriptor -> %>
${ ui.includeFragment("appointments", "program/programHistory", [ patient: patient, program: descriptor.target, showClinicalData: showClinicalData ]) }
<% } %>