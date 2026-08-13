package MikiMock.com.MikiMock.Interview.AgoraSessionTracking.service;


import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSession;
import MikiMock.com.MikiMock.Interview.AgoraSessionTracking.entity.AgoraSessionClosedBy;
import MikiMock.com.MikiMock.Interview.entity.Room;

import java.util.List;

public interface AgoraSessionServiceInt {

    AgoraSession startSession(Room room);

    void endSession(
            Room room,
            AgoraSessionClosedBy closedBy
    );

    List<AgoraSession> getActiveSessions();

    void closeExpiredSessions();

    void closeTodaySessions();

}