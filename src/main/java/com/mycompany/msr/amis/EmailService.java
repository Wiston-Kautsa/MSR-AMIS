package com.mycompany.msr.amis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class EmailService {

    private static final String ENV_SMTP_HOST = "MSR_AMIS_SMTP_HOST";
    private static final String ENV_SMTP_PORT = "MSR_AMIS_SMTP_PORT";
    private static final String ENV_SMTP_USER = "MSR_AMIS_SMTP_USERNAME";
    private static final String ENV_SMTP_PASSWORD = "MSR_AMIS_SMTP_PASSWORD";
    private static final String ENV_SMTP_FROM = "MSR_AMIS_SMTP_FROM";
    private static final String ENV_SMTP_SSL = "MSR_AMIS_SMTP_SSL";

    private EmailService() {
    }

    public static void sendPasswordResetCode(String recipientEmail, String resetCode) throws Exception {
        String smtpHost = requiredEnv(ENV_SMTP_HOST);
        String smtpPort = requiredEnv(ENV_SMTP_PORT);
        requiredEnv(ENV_SMTP_USER);
        requiredEnv(ENV_SMTP_PASSWORD);
        requiredEnv(ENV_SMTP_FROM);
        String useSsl = optionalEnv(ENV_SMTP_SSL, "true");

        String subject = "MSR AMIS password reset code";
        String body = "Your MSR AMIS password reset code is " + resetCode + ".\r\n\r\n"
                + "The code expires in 10 minutes.\r\n"
                + "If you did not request this reset, contact your administrator.";

        String script =
                "$sec = ConvertTo-SecureString $env:" + ENV_SMTP_PASSWORD + " -AsPlainText -Force; " +
                "$cred = New-Object System.Management.Automation.PSCredential($env:" + ENV_SMTP_USER + ", $sec); " +
                "$params = @{" +
                "SmtpServer='" + escapePowerShell(smtpHost) + "'; " +
                "Port=" + escapePowerShell(smtpPort) + "; " +
                "UseSsl=[System.Convert]::ToBoolean('" + escapePowerShell(useSsl) + "'); " +
                "Credential=$cred; " +
                "From=$env:" + ENV_SMTP_FROM + "; " +
                "To='" + escapePowerShell(recipientEmail) + "'; " +
                "Subject='" + escapePowerShell(subject) + "'; " +
                "Body='" + escapePowerShell(body) + "'" +
                "}; " +
                "Send-MailMessage @params";

        List<String> command = new ArrayList<>();
        command.add("powershell");
        command.add("-NoProfile");
        command.add("-Command");
        command.add(script);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Failed to send reset email. " + output.toString().trim());
        }
    }

    private static String requiredEnv(String name) throws Exception {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new Exception("Email reset is not configured. Missing environment variable: " + name);
        }
        return value.trim();
    }

    private static String optionalEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String escapePowerShell(String value) {
        return value.replace("'", "''");
    }
}
