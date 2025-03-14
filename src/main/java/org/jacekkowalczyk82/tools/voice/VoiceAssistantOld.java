package org.jacekkowalczyk82.tools.voice;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Hello world!
 */
public class VoiceAssistantOld {
    private static final int SAMPLE_RATE = 16000; // 16 kHz
    private static final int SILENCE_THRESHOLD = 1000; // Próg ciszy
    private static final int SILENCE_DURATION = 1000; // Minimalny czas ciszy (ms)


    public static void main(String[] args) {
        try {
            // Capture audio from microphone
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            // Ensure the audio format is LINEAR16 and 16000 Hz
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleRate() != 16000) {
                System.err.println("Unsupported audio format. Please provide a LINEAR16, 16000 Hz audio file.");
                return;
            }

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("Start speaking...");

            // Capture 5 seconds of audio
            long endTime = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < endTime) {
                bytesRead = microphone.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytesRead);
            }

            microphone.close();
            byte[] audioData = out.toByteArray();

            // Convert to AudioInputStream
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());

            // Save captured audio to a file in WAV format for debugging
            File wavFile = new File("captured_audio.wav");
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);

            System.out.println("Audio captured and saved to captured_audio.wav");


            // Apply noise reduction (simple noise gate filter)
            byte[] processedAudioData = applyNoiseGateFilter(audioData, format);

            // Save processed audio to a file for debugging
            ByteArrayInputStream byteArrayInputStream1 = new ByteArrayInputStream(processedAudioData);
            AudioInputStream processedAudioInputStream = new AudioInputStream(byteArrayInputStream1, format, processedAudioData.length / format.getFrameSize());
            File processedWavFile = new File("processed_audio.wav");
            AudioSystem.write(processedAudioInputStream, AudioFileFormat.Type.WAVE, processedWavFile);
            System.out.println("Processed audio saved to processed_audio.wav");




            // Send audio data to Google Cloud Speech-to-Text API
            try (SpeechClient speechClient = SpeechClient.create()) {
                ByteString audioBytes = ByteString.copyFrom(processedAudioData);

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
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }


    private static byte[] applyNoiseGateFilter(byte[] audioData, AudioFormat format) {
        // A simple noise gate filter implementation
        // This example uses a fixed threshold to reduce noise
        float threshold = 0.02f;
        ByteArrayOutputStream processedOut = new ByteArrayOutputStream();
        for (int i = 0; i < audioData.length; i += format.getFrameSize()) {
            float amplitude = 0;
            for (int j = 0; j < format.getFrameSize(); j++) {
                amplitude += audioData[i + j] * audioData[i + j];
            }
            amplitude = (float) Math.sqrt(amplitude / format.getFrameSize());
            if (amplitude > threshold) {
                for (int j = 0; j < format.getFrameSize(); j++) {
                    processedOut.write(audioData[i + j]);
                }
            } else {
                for (int j = 0; j < format.getFrameSize(); j++) {
                    processedOut.write(0);
                }
            }
        }
        return processedOut.toByteArray();
    }

//
//    public static void main(String[] args) {
//        try {
//            // Rozpocznij nagrywanie
//            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
//            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
//            System.out.println(microphone.getLineInfo());
//            microphone.open(format);
//            microphone.start();
//
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//
//            System.out.println("Nagrywanie...");
//
//            // Nagrywaj dźwięk i dziel na fragmenty
//            int counter = 0;
////            while (true) {
////                bytesRead = microphone.read(buffer, 0, buffer.length);
////                out.write(buffer, 0, bytesRead);
////
////
////
////                // Sprawdź, czy wystąpiła cisza
////                if (isSilence(buffer)) {
////                    System.out.println("Wykryto ciszę, przetwarzanie fragmentu...");
////                    byte[] audioData = out.toByteArray();
////
////                    // Save captured audio to a file for debugging
////                    try (FileOutputStream fos = new FileOutputStream(new File("captured_audio_"+counter+".wav"))) {
////                        fos.write(audioData);
////                    }
////
////
////                    out.reset();
////                    counter++;
////
////                    // Wyślij fragment do Google Cloud Speech-to-Text
////                    String transcription = transcribeAudio(audioData);
////                    System.out.println("Rozpoznany tekst: " + transcription);
////                }
////            }
//
//            // Capture 5 seconds of audio
//            long endTime = System.currentTimeMillis() + 5000;
//            while (System.currentTimeMillis() < endTime) {
//                bytesRead = microphone.read(buffer, 0, buffer.length);
//                out.write(buffer, 0, bytesRead);
//            }
//
//            microphone.close();
//            byte[] audioData = out.toByteArray();
//
//            // Convert to AudioInputStream
//            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
//            AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());
//
//            // Save captured audio to a file in WAV format for debugging
//            File wavFile = new File("captured_audio.wav");
//            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
//
//            System.out.println("Audio captured and saved to captured_audio.wav");
//
//
////             Wyślij fragment do Google Cloud Speech-to-Text
//            String transcription = transcribeAudio(audioData);
//            System.out.println("Rozpoznany tekst: " + transcription);
//
//
//        } catch (LineUnavailableException | IOException e) {
//            e.printStackTrace();
//        }
//    }

    // Sprawdź, czy bufor zawiera ciszę
    private static boolean isSilence(byte[] buffer) {
        long sum = 0;
        for (byte b : buffer) {
            sum += Math.abs(b);
        }
        double average = sum / (double) buffer.length;
        return average < SILENCE_THRESHOLD;
    }

    // Wyślij dźwięk do Google Cloud Speech-to-Text
    private static String transcribeAudio(byte[] audioData) throws IOException {
        try (SpeechClient speechClient = SpeechClient.create()) {
            ByteString audioBytes = ByteString.copyFrom(audioData);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .setLanguageCode("pl-PL") // Ustaw język (np. polski)
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);

            List<SpeechRecognitionResult> results = response.getResultsList();

            if (results.size() == 0) {
                System.out.println("WARNING:: no results");
            }

            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : results) {
                transcription.append(result.getAlternatives(0).getTranscript());
            }

//            // Check for any error details in the response
//            if (response.hasError()) {
//                Status status = response.getError();
//                System.out.println("Error code: " + status.getCode());
//                System.out.println("Error message: " + status.getMessage());
//                for (com.google.rpc.DebugInfo debugInfo : status.getDetailsList()) {
//                    System.out.println("DebugInfo: " + debugInfo);
//                }
//            }
            System.out.println("Full API response: " + response);
            return transcription.toString();
        }
    }
}
