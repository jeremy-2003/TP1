package upc.edu.chatbotIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.BlockedUser;
import upc.edu.chatbotIA.repository.BlockedUserRepository;

import java.time.LocalDateTime;

@Service
public class BlockedUserService {
    private final BlockedUserRepository blockedUserRepository;

    @Autowired
    public BlockedUserService(BlockedUserRepository blockedUserRepository) {
        this.blockedUserRepository = blockedUserRepository;
    }

    public boolean isUserBlocked(String userId) {
        BlockedUser blockedUser = blockedUserRepository.findByUserNumber(userId);
        if (blockedUser != null && blockedUser.getBlockTime().isAfter(LocalDateTime.now())) {
            return true;
        }
        return false;
    }

    public void blockUser(String userId) {
        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setUserNumber(userId);
        blockedUser.setBlockTime(LocalDateTime.now().plusMinutes(5));
        System.out.println("Esto se guardara" + blockedUser.getUserNumber() + ", " + blockedUser.getBlockTime());
        blockedUserRepository.save(blockedUser);
    }

    public BlockedUser findByUserId(String userId){
        return blockedUserRepository.findByUserNumber(userId);
    }

    public void unblockUser(String userId) {
        BlockedUser blockedUser = blockedUserRepository.findByUserNumber(userId);
        if (blockedUser != null) {
            blockedUserRepository.delete(blockedUser);
        }
    }
}