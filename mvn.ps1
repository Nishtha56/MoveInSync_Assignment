param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

& "C:\Users\nisht\apache-maven-3.9.6\bin\mvn.cmd" @Arguments
