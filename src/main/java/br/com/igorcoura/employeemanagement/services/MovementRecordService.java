package br.com.igorcoura.employeemanagement.services;

import br.com.igorcoura.employeemanagement.Mapper.EmployeeMapper;
import br.com.igorcoura.employeemanagement.domain.entities.MovementRecord;
import br.com.igorcoura.employeemanagement.domain.models.NewMovementRecordModel;
import br.com.igorcoura.employeemanagement.repository.EmployeeRepository;
import br.com.igorcoura.employeemanagement.repository.MovementRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovementRecordService {

    @Autowired
    MovementRecordRepository movementRecordRepository;
    @Autowired
    EmployeeRepository employeeemployeeRepository;

    public MovementRecord insert(NewMovementRecordModel newMovementRecordModel){
        var openList = movementRecordRepository.findAll(Example.of(MovementRecord.builder().isOpen(true).build()));
        MovementRecord movement = null;
        if(openList.stream().count() > 0){
            movement = checkMovementValidity(openList, newMovementRecordModel);
            movement = addMovementExistingRecord(movement, newMovementRecordModel);
        }
        if(movement == null){
            var employee = employeeemployeeRepository.getById(newMovementRecordModel.getIdEmployee());
            movement = MovementRecord.builder()
                    .employee(employee)
                    .startTimeWork(newMovementRecordModel.getDate())
                    .isOpen(true).build();
        }

        return movementRecordRepository.save(movement);

    }

    private MovementRecord checkMovementValidity(List<MovementRecord> listMovementRecord, NewMovementRecordModel newMovementRecordModel){

        List<MovementRecord> listValidMovement = new ArrayList<MovementRecord>();

        for(MovementRecord movement: listMovementRecord){
            if(movement.getStartTimeWork() == null){
                movement.setOpen(false);
                movementRecordRepository.save(movement);
                continue;
            }

            //Calculate date limit
            var dateLimit = movement.getStartTimeWork().plusDays(1);
            var dateTimeLimit = LocalDateTime.of(dateLimit.getYear(), dateLimit.getMonth(), dateLimit.getDayOfMonth(), movement.getEmployee().getStartWork().getHour(), movement.getEmployee().getStartWork().getMinute());

            //Check if the new movement is on the date limit
            //if it is not within the date limit, close the current movement.
            if(dateTimeLimit.isBefore(newMovementRecordModel.getDate())){
                if(movement.getEndTimeWork() == null){
                    if(movement.getEndLunchTime() != null){
                        movement.setEndLunchTime(movement.getEndLunchTime());
                    }
                    else if(movement.getStartLunchTime() != null){
                        movement.setEndTimeWork(movement.getStartLunchTime());
                    }
                }
                movement.setOpen(false);
                movementRecordRepository.save(movement);
                continue;
            }
            listValidMovement.add(movement);
        }

        MovementRecord validMovement = null;
        int countfilledField = -1;

        //if there is more than one valid case, this will choose the one with the most data filling in and close the rest.
        for(MovementRecord movement: listValidMovement){
            int count = 0;
            if(movement.getStartTimeWork() != null){
                count++;
            }
            if(movement.getStartLunchTime() != null){
                count++;
            }
            if(movement.getEndLunchTime() != null){
                count++;
            }
            if(movement.getEndTimeWork() != null){
                count++;
            }
            if(count > countfilledField){
                countfilledField = count;
                validMovement = movement;
            }
        }
        return validMovement;
    }

    private MovementRecord addMovementExistingRecord(MovementRecord movementRecord, NewMovementRecordModel newMovementRecordModel){
        if(movementRecord.getStartLunchTime() == null){
            movementRecord.setStartLunchTime(newMovementRecordModel.getDate());
            return movementRecord;
        }
        else if(movementRecord.getEndLunchTime() == null){
            movementRecord.setEndLunchTime(newMovementRecordModel.getDate());
            return movementRecord;
        }
        else if(movementRecord.getEndTimeWork() == null){
            movementRecord.setEndTimeWork(newMovementRecordModel.getDate());
            movementRecord.setOpen(false);
            return movementRecord;
        }
        return null;
    }
}