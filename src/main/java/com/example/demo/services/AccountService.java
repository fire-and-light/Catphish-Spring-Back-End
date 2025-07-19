package com.example.demo.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.Account;
import com.example.demo.models.Relationship;
import com.example.demo.repositories.AccountRepository;
import com.example.demo.repositories.MessageRepository;
import com.example.demo.repositories.RelationshipRepository;
import com.example.demo.utils.Hash;
import com.example.demo.utils.Salt;

@Service
public class AccountService {
    private BlobService bs;
    private AccountRepository ar;
    private RelationshipRepository rr;
    private MessageRepository mr;

    @Autowired
    public AccountService(BlobService bs, AccountRepository ar, RelationshipRepository rr, MessageRepository mr) {
        super();
        this.bs = bs;
        this.ar = ar;
        this.rr = rr;
        this.mr = mr;
    }

    @Transactional
    public String signUp(String username, String password) {
        Optional<Account> optional = this.ar.findById(username);

        if (!optional.isEmpty()) {
            return "An account with that username already exists";
            
        } else if (username.length() > Account.MAX_NAM_LEN) {
            return "Username exceeds " + Account.MAX_NAM_LEN + " characters";

        } else if (username.contains(" ")) {
            return "Username must not include spaces";

        } else if (username.equals("")) {
            return "Username must not be empty";
            
        } else {
            String passwordSalt = Salt.generate();
            String passwordHash = Hash.SHA384toString(password + passwordSalt);
            String bio = null;
            LocalDate dateCreated = LocalDate.now();

            try {
                File file = new File("/app/default.jpg");
                byte[] pictureBlob = Files.readAllBytes(file.toPath());
                this.bs.upload(username, pictureBlob);
                
            } catch (IOException e) {
                e.printStackTrace();
            }

            Account user = new Account(username, passwordSalt, passwordHash, bio, dateCreated, password, null);
            this.ar.save(user);

            Relationship rel = new Relationship(0, user, user, "false");
            this.rr.save(rel);

            return "Account created!";
        }
    }

    @Transactional
    public String signIn(String username, String password) {
        Optional<Account> optional = this.ar.findById(username);

        if (optional.isEmpty()) {
            return "No account with that username exists";

        } else {
            Account user = optional.get();
            String passwordSalt = user.getPasswordSalt();
            String passwordHash = Hash.SHA384toString(password + passwordSalt);

            if (!passwordHash.equals(user.getPasswordHash())) {
                return "Invalid password";
    
            } else {
                return "Signed in!";
            }
        }
    }

    @Transactional
    public Account getProfile(String username) {
        Account user = this.ar.findById(username).get();
        Account userJSON = new Account();

        userJSON.setBio(user.getBio());
        userJSON.setPictureBlob(Base64.getEncoder().encodeToString(this.bs.download(username)));

        return userJSON;
    }

    @Transactional
    public void saveProfile(Account user) {
        Account outdatedUser = this.ar.findById(user.getUsername()).get();

        user.setDateCreated(outdatedUser.getDateCreated());
        user.setPasswordHash(outdatedUser.getPasswordHash());
        user.setPasswordSalt(outdatedUser.getPasswordSalt());

        this.bs.upload(user.getUsername(), Base64.getDecoder().decode(user.getPictureBlob()));
        this.ar.save(user);
    }

    @Transactional
    public void deleteAccount(String username) {
        this.bs.delete(username);
        this.mr.deleteMessages(username);
        this.rr.deleteRelationships(username);
        this.ar.deleteById(username);
    }
}