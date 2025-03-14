package org.jacekkowalczyk82.tools.voice;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VoiceRecognition {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java VoiceRecognition <path-to-audio-file>");
            return;
        }

        String audioFilePath = args[0];

        try {
            // Read audio data from the provided file
            File audioFile = new File(audioFilePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            // Ensure the audio format is LINEAR16 and 16000 Hz
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleRate() != 16000) {
                System.err.println("Unsupported audio format. Please provide a LINEAR16, 16000 Hz audio file.");
                return;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            audioInputStream.close();
            byte[] audioData = out.toByteArray();

            // Send audio data to Google Cloud Speech-to-Text API
            try (SpeechClient speechClient = SpeechClient.create()) {
                ByteString audioBytes = ByteString.copyFrom(audioData);

                System.out.println("Audio data length: " + audioData.length);

                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000)
                        .setLanguageCode("en-US")
                        .build();

                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();

                RecognizeResponse response = speechClient.recognize(config, audio);

                // Check for errors in the response and print the full response
                if (response.getResultsList().isEmpty()) {
                    System.out.println("No speech recognized. Please check the audio input and configuration.");
                } else {
                    for (SpeechRecognitionResult result : response.getResultsList()) {
                        System.out.println("Transcript: " + result.getAlternativesList().get(0).getTranscript());
                    }
                }

                // Print the full response for debugging
                System.out.println("Full API response: " + response);
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}