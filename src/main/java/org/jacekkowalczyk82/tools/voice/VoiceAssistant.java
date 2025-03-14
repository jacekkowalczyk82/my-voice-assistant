package org.jacekkowalczyk82.tools.voice;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.jtransforms.fft.DoubleFFT_1D;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Arrays;

/**
 * VoiceAssistant
 */
public class VoiceAssistant {
    private static final int SAMPLE_RATE = 16000; // 16 kHz

    public static void main(String[] args) {
        try {
            // Capture audio from microphone
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
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

            // Apply noise reduction
            byte[] processedAudioData = applyNoiseReduction(audioData, format);

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
                        .setSampleRateHertz(SAMPLE_RATE)
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

    private static byte[] applyNoiseReduction(byte[] audioData, AudioFormat format) {
        // Apply a low-pass filter
        byte[] lowPassFilteredData = applyLowPassFilter(audioData, format, 3000); // 3 kHz cutoff

        // Apply a high-pass filter
        byte[] highPassFilteredData = applyHighPassFilter(lowPassFilteredData, format, 300); // 300 Hz cutoff

        return highPassFilteredData;
    }

    private static byte[] applyLowPassFilter(byte[] audioData, AudioFormat format, float cutoffFrequency) {
        int sampleRate = (int) format.getSampleRate();
        int dataSize = audioData.length / format.getFrameSize();
        double[] input = new double[dataSize];
        double[] output = new double[dataSize];

        // Convert byte array to double array
        for (int i = 0; i < dataSize; i++) {
            input[i] = audioData[i] / 32768.0; // Normalize to -1.0 to 1.0
        }

        // FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(dataSize);
        fft.realForward(input);

        // Apply low-pass filter
        double nyquist = 0.5 * sampleRate;
        int cutoffBin = (int) (cutoffFrequency / nyquist * (dataSize / 2));
        Arrays.fill(input, cutoffBin, dataSize - cutoffBin, 0.0);

        // Inverse FFT
        fft.realInverse(input, true);

        // Convert double array back to byte array
        byte[] filteredData = new byte[audioData.length];
        for (int i = 0; i < dataSize; i++) {
            int sample = (int) (input[i] * 32768.0);
            filteredData[i] = (byte) (sample & 0xFF);
        }

        return filteredData;
    }

    private static byte[] applyHighPassFilter(byte[] audioData, AudioFormat format, float cutoffFrequency) {
        int sampleRate = (int) format.getSampleRate();
        int dataSize = audioData.length / format.getFrameSize();
        double[] input = new double[dataSize];
        double[] output = new double[dataSize];

        // Convert byte array to double array
        for (int i = 0; i < dataSize; i++) {
            input[i] = audioData[i] / 32768.0; // Normalize to -1.0 to 1.0
        }

        // FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(dataSize);
        fft.realForward(input);

        // Apply high-pass filter
        double nyquist = 0.5 * sampleRate;
        int cutoffBin = (int) (cutoffFrequency / nyquist * (dataSize / 2));
        Arrays.fill(input, 0, cutoffBin, 0.0);

        // Inverse FFT
        fft.realInverse(input, true);

        // Convert double array back to byte array
        byte[] filteredData = new byte[audioData.length];
        for (int i = 0; i < dataSize; i++) {
            int sample = (int) (input[i] * 32768.0);
            filteredData[i] = (byte) (sample & 0xFF);
        }

        return filteredData;
    }
}