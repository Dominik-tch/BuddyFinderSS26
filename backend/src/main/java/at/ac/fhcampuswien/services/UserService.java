package at.ac.fhcampuswien.services;

import at.ac.fhcampuswien.models.User;
import at.ac.fhcampuswien.repositories.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(User newUser) {
        if (newUser.getUserName() == null || newUser.getUserName().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (newUser.getPassword() == null || newUser.getPassword().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }
        if (newUser.getEmail() == null || !newUser.getEmail().contains("@")) {
            throw new IllegalArgumentException("A valid email is required.");
        }
        // checks if userName already exists
        if (userRepository.findByUsername(newUser.getUserName()) != null) {
            throw new IllegalStateException("Username is already taken.");
        }
        String hashedPassword = hashPassword(newUser.getPassword());

        User userToSave = new User(
                newUser.getUserName(),
                hashedPassword,
                newUser.getEmail(),
                newUser.getFirstName(),
                newUser.getLastName()
        );
        userRepository.add(userToSave);
    }

    public void updateUser(User userToUpdate) {
        if (userToUpdate.getEmail() == null || !userToUpdate.getEmail().contains("@")) {
            throw new IllegalArgumentException("A valid email is required.");
        }
        userRepository.update(userToUpdate);
    }

    public User authenticateUser(String username, String plainTextPassword) {
        if (username == null || plainTextPassword == null) {
            return null;
        }

        User dbUser = userRepository.findByUsername(username);

        // if user exists check password
        if (dbUser != null) {
            // Hash incoming password and check with hashed password from database
            String hashedInput = hashPassword(plainTextPassword);

            if (dbUser.getPassword().equals(hashedInput)) {
                return dbUser; // Login succesful
            }
        }
        return null; // Wrong password or username
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}